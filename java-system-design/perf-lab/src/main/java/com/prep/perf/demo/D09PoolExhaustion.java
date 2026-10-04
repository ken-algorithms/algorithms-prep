package com.prep.perf.demo;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * P09 - Goi HTTP ra ngoai BEN TRONG @Transactional -> giu DB connection suot thoi gian cho mang.
 *
 * <pre>
 * XAU   @Transactional void transfer() { read(); partner.call(); write(); }   // giu conn 5+50+5 ms
 * SUA   tx1: read()  ->  partner.call() ngoai tx  ->  tx2: write()           // giu conn 5+5 ms
 *       (kem idempotency key + outbox de tx2 an toan khi tx1 da commit)
 * </pre>
 *
 * Little's Law: so connection dang ban = throughput x thoi gian giu. Pool 10, giu 60 ms
 * -> tran 10 / 0.060 = ~166 req/s cho CA service, bat ke bao nhieu pod, CPU hay thread.
 * Mo phong: Semaphore = HikariCP (fair, cho toi connectionTimeout), sleep = cho IO.
 */
public final class D09PoolExhaustion {

    private D09PoolExhaustion() {}

    public record Result(String mode, int requests, long wallMs, double rps, String latency,
                         long p99ConnWaitMs, int maxInUse) {}

    public static Result run(boolean holdDuringRemoteCall, int requests, int clients, int poolSize,
                             Duration dbStep, Duration remoteCall) {
        var pool = new Semaphore(poolSize, true);
        var lat = new Lat();
        var connWait = new Lat();
        var inUse = new AtomicInteger();
        var maxInUse = new AtomicInteger();
        var next = new AtomicInteger();
        long start = System.nanoTime();

        try (ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int c = 0; c < clients; c++) {
                ex.submit(() -> {
                    while (next.getAndIncrement() < requests) {
                        long t0 = System.nanoTime();
                        if (holdDuringRemoteCall) {
                            withConnection(pool, connWait, inUse, maxInUse, () -> {
                                Lat.sleepMs(dbStep.toMillis());       // SELECT ... FOR UPDATE
                                Lat.sleepMs(remoteCall.toMillis());   // goi partner/core banking
                                Lat.sleepMs(dbStep.toMillis());       // UPDATE + INSERT
                            });
                        } else {
                            withConnection(pool, connWait, inUse, maxInUse, () -> Lat.sleepMs(dbStep.toMillis()));
                            Lat.sleepMs(remoteCall.toMillis());
                            withConnection(pool, connWait, inUse, maxInUse, () -> Lat.sleepMs(dbStep.toMillis()));
                        }
                        lat.add(System.nanoTime() - t0);
                    }
                    return null;
                });
            }
        }
        long wall = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        return new Result(holdDuringRemoteCall ? "remote call INSIDE tx" : "remote call OUTSIDE tx",
                requests, wall, requests * 1000.0 / wall, lat.summary(), connWait.pMs(99), maxInUse.get());
    }

    private static void withConnection(Semaphore pool, Lat connWait, AtomicInteger inUse,
                                       AtomicInteger maxInUse, Runnable body) {
        long w0 = System.nanoTime();
        try {
            if (!pool.tryAcquire(30, TimeUnit.SECONDS)) {   // HikariCP connectionTimeout mac dinh 30s
                throw new IllegalStateException("Connection is not available, request timed out after 30000ms");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        connWait.add(System.nanoTime() - w0);
        maxInUse.accumulateAndGet(inUse.incrementAndGet(), Math::max);
        try {
            body.run();
        } finally {
            inUse.decrementAndGet();
            pool.release();
        }
    }

    public static void main(String[] args) {
        int requests = 1000, clients = 200, pool = 10;
        var db = Duration.ofMillis(5);
        var remote = Duration.ofMillis(50);
        System.out.printf("pool=%d, clients=%d, db step=%dms x2, remote=%dms%n", pool, clients, db.toMillis(), remote.toMillis());
        for (boolean bad : new boolean[]{true, false}) {
            System.out.println(run(bad, requests, clients, pool, db, remote));
        }
    }
}
