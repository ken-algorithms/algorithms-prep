package com.prep.onbai.pgconcurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.prep.onbai.pgconcurrency.jpa.AuditLog;
import com.prep.onbai.pgconcurrency.jpa.Customer;
import com.prep.onbai.pgconcurrency.jpa.JpaLabApp;
import com.prep.onbai.pgconcurrency.jpa.AuditRepository;
import com.prep.onbai.pgconcurrency.jpa.CustomerRepository;
import com.prep.onbai.pgconcurrency.jpa.WalletRepository;
import com.prep.onbai.pgconcurrency.jpa.RetryingWallets;
import com.prep.onbai.pgconcurrency.jpa.SqlCapture;
import com.prep.onbai.pgconcurrency.jpa.Wallet;
import com.prep.onbai.pgconcurrency.jpa.WalletService;
import com.prep.onbai.testsupport.Pg;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/** Bai 04 — cac bay cua Spring @Transactional va Spring Data JPA, tren Postgres that. */
@SpringBootTest(classes = JpaLabApp.class, webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.prep.onbai.pgconcurrency.jpa.SqlCapture",
        "spring.datasource.hikari.maximum-pool-size=20",
        "logging.level.org.hibernate.engine.jdbc.spi.SqlExceptionHelper=OFF"})
class JpaPitfallsTest {

    static final String URL = Pg.freshDatabaseUrl("b04_jpa"); // tinh MOT lan: supplier ben duoi co the bi goi nhieu lan

    @DynamicPropertySource
    static void db(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> URL);
        r.add("spring.datasource.username", () -> "postgres");
    }

    @Autowired WalletService walletService;
    @Autowired RetryingWallets retrying;
    @Autowired WalletRepository wallets;
    @Autowired CustomerRepository customers;
    @Autowired AuditRepository audits;
    @Autowired TransactionTemplate tx;

    @BeforeEach
    void seed() {
        audits.deleteAllInBatch();
        wallets.deleteAllInBatch();
        customers.deleteAllInBatch();
        for (long c = 1; c <= 10; c++) {
            Customer customer = customers.save(new Customer(c, "khach " + c));
            for (long w = 0; w < 3; w++) {
                wallets.save(new Wallet(c * 10 + w, customer, 100));
            }
        }
    }

    @Test
    void optimisticLockingThrowsOnConflictAndRetryOutsideTheTransactionRecovers() throws Exception {
        int n = 8;
        AtomicInteger failedWithoutRetry = new AtomicInteger();
        runConcurrently(n, i -> {
            try {
                walletService.withdrawOptimistic(10L, 5);
            } catch (ObjectOptimisticLockingFailureException e) {
                failedWithoutRetry.incrementAndGet();
            }
        });
        long afterNoRetry = wallets.findById(10L).orElseThrow().getBalance();

        AtomicInteger attempts = new AtomicInteger();
        runConcurrently(n, i -> attempts.addAndGet(retrying.withdraw(11L, 5, 20)));
        long afterRetry = wallets.findById(11L).orElseThrow().getBalance();

        System.out.printf("[LAB] @Version: khong retry -> %d/%d lenh no OptimisticLock, so du %d | co retry -> %d lan thu cho %d lenh, so du %d%n",
                failedWithoutRetry.get(), n, afterNoRetry, attempts.get(), n, afterRetry);
        assertThat(afterNoRetry).isEqualTo(100 - 5L * (n - failedWithoutRetry.get())); // khong mat update nao
        assertThat(afterRetry).isEqualTo(100 - 5L * n);
    }

    @Test
    void lockAnnotationGeneratesARowLock() {
        SqlCapture.SQL.clear();
        walletService.withdrawPessimistic(10L, 30);
        String select = SqlCapture.SQL.stream().filter(s -> s.startsWith("select")).findFirst().orElseThrow();
        System.out.println("[LAB] @Lock(PESSIMISTIC_WRITE) sinh ra: " + select);
        // Hibernate 6 + Postgres: "for no key update" — van loai tru moi writer khac tren dong nay,
        // nhung KHONG chan FK check (FOR KEY SHARE) khi bang con insert dong tro toi no -> it deadlock hon.
        assertThat(select).endsWith("for no key update");
        assertThat(wallets.findById(10L).orElseThrow().getBalance()).isEqualTo(70);
    }

    @Test
    void atomicConditionalUpdateNeverOverdrawsUnderContention() throws Exception {
        AtomicInteger ok = new AtomicInteger();
        runConcurrently(30, i -> {
            if (walletService.withdrawAtomic(12L, 10)) {
                ok.incrementAndGet();
            }
        });
        assertThat(ok.get()).isEqualTo(10);
        assertThat(wallets.findById(12L).orElseThrow().getBalance()).isZero();
    }

    @Test
    void selfInvocationSilentlyIgnoresRequiresNew() {
        assertThatThrownBy(() -> walletService.withdrawWithSelfInvokedAudit(10L, 1_000)).hasMessageContaining("khong du tien");
        long viaThis = audits.count();
        assertThatThrownBy(() -> walletService.withdrawWithAudit(10L, 1_000)).hasMessageContaining("khong du tien");
        List<String> kept = audits.findAll().stream().map(AuditLog::getMessage).toList();
        System.out.printf("[LAB] REQUIRES_NEW goi qua this: con %d dong audit | qua bean khac: con %s%n", viaThis, kept);
        assertThat(viaThis).isZero();   // audit bi rollback chung voi nghiep vu
        assertThat(kept).hasSize(1);    // audit song sot
    }

    @Test
    void lazyCollectionCausesNPlusOneAndAnEntityGraphFixesIt() {
        int plain = countStatements(() -> customers.findAllPlain().forEach(c -> c.getWallets().size()));
        int graph = countStatements(() -> customers.findAllWithWallets().forEach(c -> c.getWallets().size()));
        System.out.printf("[LAB] 10 khach x 3 vi: LAZY -> %d cau SQL (1 + N), @EntityGraph -> %d cau%n", plain, graph);
        assertThat(plain).isEqualTo(11);
        assertThat(graph).isEqualTo(1);
    }

    private int countStatements(Runnable work) {
        SqlCapture.SQL.clear();
        tx.executeWithoutResult(s -> work.run()); // trong transaction: lazy load duoc (open-in-view da tat)
        return SqlCapture.SQL.size();
    }

    interface Job {
        void run(int i) throws Exception;
    }

    private static void runConcurrently(int n, Job job) throws Exception {
        CyclicBarrier start = new CyclicBarrier(n);
        List<Future<?>> fs = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(n)) {
            for (int i = 0; i < n; i++) {
                int k = i;
                fs.add(pool.submit(() -> {
                    start.await();
                    job.run(k);
                    return null;
                }));
            }
        }
        for (Future<?> f : fs) {
            f.get();
        }
    }
}
