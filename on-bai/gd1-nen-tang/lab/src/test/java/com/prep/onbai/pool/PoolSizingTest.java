package com.prep.onbai.pool;

import static org.assertj.core.api.Assertions.assertThat;

import com.prep.onbai.pool.QueueSimulator.Config;
import com.prep.onbai.pool.QueueSimulator.Result;
import com.prep.onbai.testsupport.Pg;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/** Bai 01 — Little's Law va kich thuoc pool: so lieu tat dinh tu mo phong + mot deadlock pool that. */
class PoolSizingTest {

    static final double INF = Double.POSITIVE_INFINITY;

    @Test
    void littlesLawArithmetic() {
        assertThat(LittlesLaw.inFlight(2_000, 50)).isEqualTo(100.0);
        assertThat(LittlesLaw.maxThroughput(10, 55)).isCloseTo(181.8, org.assertj.core.data.Offset.offset(0.1));
        assertThat(LittlesLaw.requiredPool(2_000, 5, 0.7)).isEqualTo(15);
        assertThat(LittlesLaw.deadlockFreePool(8, 2)).isEqualTo(9);
    }

    @Test
    void latencyExplodesAsUtilizationApproachesOne() {
        double[] rhos = {0.5, 0.7, 0.8, 0.9, 0.95, 0.99};
        List<Result> rows = new ArrayList<>();
        for (double rho : rhos) { // 10 connection, giu trung binh 10 ms -> nang luc 1.000 req/s
            rows.add(QueueSimulator.run(new Config(rho * 1000, 10, 10, INF, 300_000, 42)));
        }
        rows.forEach(r -> System.out.println("[LAB] 10 conn, giu 10ms: " + r));
        for (int i = 1; i < rows.size(); i++) {
            assertThat(rows.get(i).p99Ms()).isGreaterThan(rows.get(i - 1).p99Ms());
        }
        assertThat(rows.get(5).p99Ms()).isGreaterThan(5 * rows.get(0).p99Ms()); // 99% tai: p99 gap nhieu lan
    }

    @Test
    void holdingTheConnectionDuringARemoteCallCapsThroughput() {
        // cung 300 req/s, cung pool 10: giu 5 ms (chi DB) vs 55 ms (DB + goi doi tac trong transaction)
        Result dbOnly = QueueSimulator.run(new Config(300, 5, 10, 1_000, 100_000, 7));
        Result withRemote = QueueSimulator.run(new Config(300, 55, 10, 1_000, 100_000, 7));
        System.out.println("[LAB] giu 5ms : " + dbOnly);
        System.out.println("[LAB] giu 55ms: " + withRemote + " | tran Little = " + Math.round(LittlesLaw.maxThroughput(10, 55)) + "/s");
        assertThat(dbOnly.timedOut()).isZero();
        assertThat(withRemote.throughputPerSec()).isBetween(160.0, 190.0); // ket o tran 10 / 0,055 s
        assertThat(withRemote.timedOut()).isGreaterThan(30_000);           // phan con lai bo cuoc sau 1 s cho
    }

    @Test
    void aBoundedWaitShedsLoadButKeepsLatencyBounded() {
        Config overload = new Config(1_500, 10, 10, INF, 60_000, 3);        // 150% nang luc
        Result unbounded = QueueSimulator.run(overload);
        Result shed = QueueSimulator.run(new Config(1_500, 10, 10, 100, 60_000, 3));
        System.out.println("[LAB] qua tai 150%, cho vo han : " + unbounded);
        System.out.println("[LAB] qua tai 150%, cho <= 100ms: " + shed);
        assertThat(unbounded.p99Ms()).isGreaterThan(10_000);               // hang doi phinh mai: ai cung cham
        assertThat(shed.p99WaitMs()).isLessThanOrEqualTo(100);
        double shedRatio = (double) shed.timedOut() / 60_000;
        assertThat(shedRatio).isBetween(0.25, 0.45);                        // ~1/3 bi tu choi nhanh, 2/3 duoc phuc vu tot
    }

    @Test
    void requiresNewDeadlocksAPoolThatIsTooSmall() throws Exception {
        String url = Pg.freshDatabaseUrl("b01_pool");
        String small = run(url, 4, 4);  // 4 request, pool 4: ai cung giu 1, cho them 1
        String sized = run(url, 4, LittlesLaw.deadlockFreePool(4, 2)); // 4 x (2 - 1) + 1 = 5
        System.out.println("[LAB] REQUIRES_NEW, 4 request: pool 4 -> " + small + " | pool 5 -> " + sized);
        assertThat(small).contains("timeout");
        assertThat(sized).isEqualTo("4 ok");
    }

    private static String run(String url, int requests, int poolSize) throws Exception {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername("postgres");
        cfg.setMaximumPoolSize(poolSize);
        cfg.setConnectionTimeout(500); // mac dinh 30 s: test se doi 30 s moi thay loi
        try (HikariDataSource ds = new HikariDataSource(cfg)) {
            NestedTransactions app = new NestedTransactions(ds);
            CyclicBarrier allHoldFirst = new CyclicBarrier(requests);
            List<Future<?>> fs = new ArrayList<>();
            try (ExecutorService pool = Executors.newFixedThreadPool(requests)) {
                for (int i = 0; i < requests; i++) {
                    fs.add(pool.submit(() -> app.handleRequest(allHoldFirst)));
                }
            }
            int ok = 0, timeout = 0;
            for (Future<?> f : fs) {
                try {
                    f.get();
                    ok++;
                } catch (java.util.concurrent.ExecutionException e) {
                    timeout++; // CannotCreateTransactionException <- SQLTransientConnectionException (Hikari timeout)
                }
            }
            return timeout == 0 ? ok + " ok" : ok + " ok, " + timeout + " timeout";
        }
    }
}
