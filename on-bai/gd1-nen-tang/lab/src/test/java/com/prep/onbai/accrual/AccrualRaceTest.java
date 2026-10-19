package com.prep.onbai.accrual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.prep.onbai.accrual.LoanBook.Loan;
import com.prep.onbai.ledger.BalanceQueries;
import com.prep.onbai.ledger.Ledger.Receipt;
import com.prep.onbai.ledger.LedgerReconciler;
import com.prep.onbai.testsupport.Pg;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Bai 06 — race giua job tinh lai va thanh toan (doc 06), tren Postgres that.
 * Du no goc 10.000, lai 0,05%/ngay: lai dung la 5,00 (tinh truoc khi tra) hoac 3,50 (sau khi tra 3.000).
 */
class AccrualRaceTest {

    static final LocalDate D = LocalDate.of(2026, 10, 18);
    static final BigDecimal RATE = new BigDecimal("0.0005");
    static final BigDecimal TEN_K = new BigDecimal("10000"), THREE_K = new BigDecimal("3000");

    DataSource ds;
    JdbcTemplate jdbc;
    LoanBook book;
    ExecutorService other;

    @BeforeEach
    void fresh() {
        ds = Pg.freshDatabase("b06_accrual", 32, "ledger.sql", "accrual.sql");
        jdbc = new JdbcTemplate(ds);
        book = new LoanBook(ds);
        book.openSystemAccounts();
        other = Executors.newCachedThreadPool();
    }

    @AfterEach
    void stop() {
        other.shutdownNow();
    }

    Loan loanWith10k(long id) {
        Loan loan = book.open(id, RATE);
        book.disburse(loan, TEN_K, D.minusDays(10));
        return loan;
    }

    BigDecimal posted(Loan loan, LocalDate day) {
        return EffectiveDateAccrual.postedInterest(jdbc, loan, day);
    }

    BigDecimal sumOf(long account) {
        return jdbc.queryForObject("SELECT coalesce(sum(amount), 0) FROM posting WHERE account_id = ?", BigDecimal.class, account);
    }

    // ── 1. Tai hien loi that ───────────────────────────────────────────────────────────────────

    @Test
    void legacyJobComputesOnTheStaleBalanceAndOverwritesTheColumn() {
        Loan loan = loanWith10k(42);
        new LegacyAccrualJob(ds).run(List.of(loan), D, () -> book.pay(loan, "p1", THREE_K, D)); // khach tra giua luc job chay

        BigDecimal column = jdbc.queryForObject("SELECT balance FROM account WHERE id = ?", BigDecimal.class, loan.principal());
        System.out.printf("[LAB] job cu: lai ghi %s (dung: 3.50), cot balance %s, SUM(posting) %s%n",
                plain(posted(loan, D)), plain(column), plain(sumOf(loan.principal())));
        assertThat(posted(loan, D)).isEqualByComparingTo("5.00");               // loi A: tinh tren so du cu
        assertThat(column).isEqualByComparingTo("10000");                       // loi B: de nguoc so du
        assertThat(sumOf(loan.principal())).isEqualByComparingTo("7000");
        // khong exception nao, khong log loi nao. Chi doi soat moi thay:
        assertThat(new LedgerReconciler(ds).run().balanceDrift()).containsExactly(loan.principal());
        assertThat(new AccrualAudit(ds).check(D)).containsExactly(
                new AccrualAudit.Mismatch(42, D, new BigDecimal("3.50"), new BigDecimal("5.0000")));
    }

    @Test
    void oneReadCommittedTransactionIsNotEnough() {
        Loan loan = loanWith10k(42);
        long[] paymentJournal = new long[1];
        // @Transactional mac dinh READ COMMITTED: doc khong khoa -> thanh toan chay xuyen qua, khong cho ai
        RcTransactionAccrual.Result r = new RcTransactionAccrual(ds).accrueOne(loan, D,
                () -> paymentJournal[0] = inOtherThread(() -> book.pay(loan, "p1", THREE_K, D)).journalId());

        System.out.printf("[LAB] 1 transaction READ COMMITTED: lai %s tinh tren %s @seq %d, luc commit seq da la %d%n",
                plain(r.interest()), plain(r.basis()), r.basisSeq(), r.seqAtCommit());
        assertThat(r.interest()).isEqualByComparingTo("5.00");
        assertThat(r.staleAtCommit()).isTrue();                       // TOCTOU: doc seq 1, ghi khi da la seq 2
        assertThat(r.journalId()).isGreaterThan(paymentJournal[0]);   // ghi SAU thanh toan, ma tinh TRUOC no
        assertThat(new AccrualAudit(ds).check(D)).hasSize(1);
    }

    @Test
    void repeatableReadTurnsTheSilentOverwriteInto40001() throws SQLException {
        Loan loan = loanWith10k(42);
        // REPEATABLE READ (hay SERIALIZABLE): ghi de dong ma nguoi khac da commit sau snapshot -> 40001
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            BigDecimal read = balanceOf(c, loan.principal());
            book.pay(loan, "p1", THREE_K, D);
            SQLException e = org.junit.jupiter.api.Assertions.assertThrows(SQLException.class,
                    () -> overwrite(c, loan.principal(), read));
            System.out.println("[LAB] REPEATABLE READ ghi de: " + e.getSQLState() + " " + e.getMessage().lines().findFirst().orElse(""));
            assertThat(e.getSQLState()).isEqualTo("40001"); // -> phai co retry, ma retry thi phai idempotent
            c.rollback();
        }
        // cung doan code o READ COMMITTED: "thanh cong", va sai
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            BigDecimal read = balanceOf(c, loan.principal());
            book.pay(loan, "p2", THREE_K, D);
            overwrite(c, loan.principal(), read);
            c.commit();
        }
        assertThat(new LedgerReconciler(ds).run().balanceDrift()).containsExactly(loan.principal());
    }

    @Test
    void retryWithoutAnIdempotencyKeyChargesInterestTwice() {
        Loan legacy = loanWith10k(42);
        LegacyAccrualJob job = new LegacyAccrualJob(ds);
        job.postInterest(legacy, D, new BigDecimal("5.00"));
        job.postInterest(legacy, D, new BigDecimal("5.00")); // lan 1 da commit nhung client timeout -> retry
        assertThat(posted(legacy, D)).isEqualByComparingTo("10.00");

        Loan fixed = loanWith10k(43);
        LockedAccrualService locked = new LockedAccrualService(ds);
        LockedAccrualService.Result first = locked.accrueOne(fixed, D);
        LockedAccrualService.Result retry = locked.accrueOne(fixed, D);
        System.out.printf("[LAB] retry: key ngau nhien -> lai %s; key accrual:43:%s -> lai %s, lan 2 alreadyDone=%s%n",
                plain(posted(legacy, D)), D, plain(posted(fixed, D)), retry.alreadyDone());
        assertThat(first.alreadyDone()).isFalse();
        assertThat(retry.alreadyDone()).isTrue();
        assertThat(posted(fixed, D)).isEqualByComparingTo("5.00");
    }

    // ── 2. Ban va: khoa bi quan theo account ───────────────────────────────────────────────────

    @Test
    void whileTheAccrualHoldsTheLockThePaymentWaitsAndViceVersa() throws Exception {
        LockedAccrualService locked = new LockedAccrualService(ds);
        TransactionTemplate outer = new TransactionTemplate(new DataSourceTransactionManager(ds));

        // (a) accrual khoa truoc: thanh toan phai CHO toi khi accrual commit
        Loan a = loanWith10k(42);
        List<Future<Receipt>> payment = new ArrayList<>();
        LockedAccrualService.Result acc = outer.execute(s -> {
            LockedAccrualService.Result r = locked.accrueOne(a, D); // tham gia transaction ngoai: khoa con giu
            payment.add(other.submit(() -> book.pay(a, "pa", THREE_K, D)));
            awaitLockWaiters(1);                                     // thay no dang cho khoa trong pg_stat_activity
            return r;
        });
        Receipt paid = payment.get(0).get(10, TimeUnit.SECONDS);
        assertThat(acc.basisSeq()).isEqualTo(1);
        assertThat(acc.interest()).isEqualByComparingTo("5.00");
        assertThat(paid.on(a.principal()).seq()).isEqualTo(2);      // thanh toan xep SAU accrual: nhat quan

        // (b) thanh toan khoa truoc: accrual phai CHO, roi tinh tren so du da tru
        Loan b = loanWith10k(43);
        List<Future<LockedAccrualService.Result>> accrual = new ArrayList<>();
        outer.executeWithoutResult(s -> {
            book.pay(b, "pb", THREE_K, D);
            accrual.add(other.submit(() -> locked.accrueOne(b, D)));
            awaitLockWaiters(1);
        });
        LockedAccrualService.Result after = accrual.get(0).get(10, TimeUnit.SECONDS);
        System.out.printf("[LAB] khoa bi quan: accrual truoc -> %s @seq %d; thanh toan truoc -> %s @seq %d%n",
                plain(acc.interest()), acc.basisSeq(), plain(after.interest()), after.basisSeq());
        assertThat(after.basisSeq()).isEqualTo(2);
        assertThat(after.interest()).isEqualByComparingTo("3.50");
    }

    @Test
    void underRealConcurrencyEveryOutcomeIsOneOfTheTwoSerialOrders() throws Exception {
        LockedAccrualService locked = new LockedAccrualService(ds);
        RcTransactionAccrual rc = new RcTransactionAccrual(ds);
        BalanceQueries q = new BalanceQueries(ds);
        Map<String, Integer> lockedOutcomes = new TreeMap<>();
        int rcStale = 0, rounds = 20;
        for (int round = 0; round < rounds; round++) {
            int i = round;
            Loan l1 = loanWith10k(100 + i);
            CyclicBarrier go = new CyclicBarrier(2);
            Future<LockedAccrualService.Result> acc = other.submit(() -> {
                go.await();
                sleep(java.util.concurrent.ThreadLocalRandom.current().nextInt(0, 4)); // xao tron ai toi truoc
                return locked.accrueOne(l1, D);
            });
            Future<Receipt> pay = other.submit(() -> {
                go.await();
                return book.pay(l1, "p" + i, THREE_K, D);
            });
            LockedAccrualService.Result r = acc.get(10, TimeUnit.SECONDS);
            pay.get(10, TimeUnit.SECONDS);
            lockedOutcomes.merge(plain(r.interest()) + "@seq" + r.basisSeq(), 1, Integer::sum);
            assertThat(r.interest()).isIn(new BigDecimal("5.00"), new BigDecimal("3.50"));
            assertThat(q.asOfSeq(l1.principal(), r.basisSeq())).isEqualByComparingTo(r.basis()); // basis = dung trang thai tai seq do

            Loan l2 = loanWith10k(200 + i); // cung kich ban, ban READ COMMITTED khong khoa, co khoang ho 20 ms
            CyclicBarrier go2 = new CyclicBarrier(2);
            Future<RcTransactionAccrual.Result> acc2 = other.submit(() -> {
                go2.await();
                return rc.accrueOne(l2, D, () -> sleep(20));
            });
            Future<Receipt> pay2 = other.submit(() -> {
                go2.await();
                return book.pay(l2, "q" + i, THREE_K, D);
            });
            rcStale += acc2.get(10, TimeUnit.SECONDS).staleAtCommit() ? 1 : 0;
            pay2.get(10, TimeUnit.SECONDS);
        }
        System.out.printf("[LAB] %d vong tranh chap that: ban khoa -> %s, 0 lan tinh tren so du cu; ban READ COMMITTED -> %d/%d lan tinh tren so du cu%n",
                rounds, lockedOutcomes, rcStale, rounds);
        assertThat(new LedgerReconciler(ds).run().clean()).isTrue();
    }

    @Test
    void skipLockedBatchNeverWaitsForACustomerAndCatchesUpNextPass() throws Exception {
        List<Loan> loans = new ArrayList<>();
        for (long id = 1; id <= 6; id++) {
            loans.add(loanWith10k(id));
        }
        Loan busy = loans.get(2); // khoan vay 3 dang co giao dich chua commit
        CountDownLatch holding = new CountDownLatch(1), release = new CountDownLatch(1);
        TransactionTemplate outer = new TransactionTemplate(new DataSourceTransactionManager(ds));
        Future<?> customer = other.submit(() -> outer.executeWithoutResult(s -> {
            book.pay(busy, "slow", THREE_K, D);
            holding.countDown();
            awaitQuietly(release);
        }));
        assertThat(holding.await(10, TimeUnit.SECONDS)).isTrue();

        SkipLockedAccrualBatch batch = new SkipLockedAccrualBatch(ds);
        long t0 = System.nanoTime();
        SkipLockedAccrualBatch.Pass p1 = batch.runPass(loans, D, 2);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        release.countDown();
        customer.get(10, TimeUnit.SECONDS);
        SkipLockedAccrualBatch.Pass p2 = batch.runPass(loans, D, 2);
        SkipLockedAccrualBatch.Pass p3 = batch.runPass(loans, D, 2);

        System.out.printf("[LAB] SKIP LOCKED: vong 1 (%d ms) %s | vong 2 %s | vong 3 %s%n", ms, p1, p2, p3);
        assertThat(p1.skipped()).containsExactly(3L);
        assertThat(p1.accrued()).containsExactly(1L, 2L, 4L, 5L, 6L);
        assertThat(ms).isLessThan(3000); // khong cho giao dich cua khach
        assertThat(p2.accrued()).containsExactly(3L);
        assertThat(p3.alreadyDone()).hasSize(6);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM journal WHERE kind = 'ACCRUAL'", Long.class)).isEqualTo(6);
        assertThat(posted(busy, D)).isEqualByComparingTo("3.50"); // tinh SAU khi khach tra xong
    }

    // ── 3. Thiet ke lai: doi dinh nghia ────────────────────────────────────────────────────────

    @Test
    void cutoffDecidesTheBusinessDateNotTheJobsClock() {
        assertThat(LoanBook.effectiveDate(at(D, LocalTime.of(18, 59, 59)))).isEqualTo(D);
        assertThat(LoanBook.effectiveDate(at(D, LocalTime.of(19, 0, 3)))).isEqualTo(D.plusDays(1));
    }

    @Test
    void withTheEffectiveDateDefinitionTheOrderNoLongerMatters() throws Exception {
        EffectiveDateAccrual accrual = new EffectiveDateAccrual(ds);
        LocalDate eff = LoanBook.effectiveDate(at(D, LocalTime.of(19, 0, 3))); // tra 19:00:03 -> thuoc ngay D+1

        Loan first = loanWith10k(1);
        accrual.accrue(first, D);
        book.pay(first, "a", THREE_K, eff);
        Loan second = loanWith10k(2);
        book.pay(second, "b", THREE_K, eff);
        accrual.accrue(second, D);
        List<Loan> raced = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Loan l = loanWith10k(10 + i);
            raced.add(l);
            CyclicBarrier go = new CyclicBarrier(2);
            Future<?> x = other.submit(() -> {
                go.await();
                return accrual.accrue(l, D);
            });
            Future<?> y = other.submit(() -> {
                go.await();
                return book.pay(l, "r" + l.id(), THREE_K, eff);
            });
            x.get(10, TimeUnit.SECONDS);
            y.get(10, TimeUnit.SECONDS);
        }
        assertThat(posted(first, D)).isEqualByComparingTo("5.00");
        assertThat(posted(second, D)).isEqualByComparingTo("5.00");
        assertThat(raced).allSatisfy(l -> assertThat(posted(l, D)).isEqualByComparingTo("5.00"));
        for (Loan l : raced) {
            accrual.accrue(l, D.plusDays(1));
        }
        assertThat(raced).allSatisfy(l -> assertThat(posted(l, D.plusDays(1))).isEqualByComparingTo("3.50"));
        System.out.println("[LAB] dinh nghia theo ngay nghiep vu: 22/22 khoan vay lai ngay D = 5.00 bat ke thu tu, ngay D+1 = 3.50");
        assertThat(new AccrualAudit(ds).check(D)).isEmpty();
    }

    @Test
    void aBackdatedPaymentIsFixedByAnAdjustmentNotByALock() throws Exception {
        EffectiveDateAccrual accrual = new EffectiveDateAccrual(ds);
        AccrualAudit audit = new AccrualAudit(ds);
        Loan loan = loanWith10k(42);
        accrual.accrue(loan, D);
        book.pay(loan, "bank-file", THREE_K, D); // sang hom sau file ngan hang ve: khoan tra nay thuoc ngay D
        assertThat(audit.check(D)).hasSize(1);

        // 8 ban doi soat chay cung luc: dung MOT but toan dieu chinh duoc ghi
        CyclicBarrier go = new CyclicBarrier(8);
        List<Future<?>> runs = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            runs.add(other.submit(() -> {
                go.await();
                return accrual.reconcile(loan, D);
            }));
        }
        for (Future<?> f : runs) {
            f.get(10, TimeUnit.SECONDS);
        }
        List<Map<String, Object>> adj = jdbc.queryForList(
                "SELECT j.idempotency_key, a.interest FROM accrual a JOIN journal j ON j.id = a.journal_id WHERE a.kind = 'ADJUSTMENT'");
        System.out.println("[LAB] ghi muon -> dieu chinh: " + adj + ", tong lai ngay D = " + plain(posted(loan, D)));
        assertThat(adj).hasSize(1);
        assertThat((BigDecimal) adj.get(0).get("interest")).isEqualByComparingTo("-1.50");
        assertThat(posted(loan, D)).isEqualByComparingTo("3.50");
        assertThat(accrual.reconcile(loan, D)).isEmpty();
        assertThat(audit.check(D)).isEmpty();
        assertThat(new LedgerReconciler(ds).run().clean()).isTrue();
    }

    // ── helpers ────────────────────────────────────────────────────────────────────────────────

    private <T> T inOtherThread(java.util.concurrent.Callable<T> work) {
        try {
            return other.submit(work).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Doi toi khi co n session dang cho khoa (wait_event_type = 'Lock') trong database nay.
     * Hoi tren connection RIENG, autocommit: trong mot transaction, pg_stat_activity bi chup mot lan
     * roi giu nguyen toi cuoi transaction, hoi lai bao nhieu lan cung ra so cu.
     */
    private void awaitLockWaiters(int n) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT count(*) FROM pg_stat_activity WHERE datname = current_database() AND wait_event_type = 'Lock'")) {
            while (System.nanoTime() < deadline) {
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) >= n) {
                        return;
                    }
                }
                sleep(20);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        throw new AssertionError("khong thay session nao cho khoa");
    }

    private static BigDecimal balanceOf(Connection c, long account) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT balance FROM account WHERE id = ?")) {
            ps.setLong(1, account);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }

    private static void overwrite(Connection c, long account, BigDecimal value) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE account SET balance = ? WHERE id = ?")) {
            ps.setBigDecimal(1, value);
            ps.setLong(2, account);
            ps.executeUpdate();
        }
    }

    private static java.time.Instant at(LocalDate day, LocalTime time) {
        return ZonedDateTime.of(day, time, LoanBook.ZONE).toInstant();
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
