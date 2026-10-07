package com.prep.ops;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

/**
 * Retry storm: retry lam su co NANG THEM. Mot downstream co nang luc huu han bi su co 2 giay; client
 * goi voi 4 chinh sach khac nhau. Do: so lan goi thuc su vao downstream (khuech dai), ty le thanh cong
 * trong va sau su co, p99.
 *
 * <pre>
 * downstream   20 slot dong thoi x 10 ms = 2.000 req/s; het slot -> tu choi NGAY (503)
 *              su co t = 2..4 s: moi lan goi chiem slot 100 ms roi loi (timeout phia sau)
 * client       1.000 req/s trong 8 s, deadline 1 s moi request
 * </pre>
 */
public final class RetryStorm {

    private RetryStorm() {}

    static final int RPS = 1_000;
    static final int SECONDS = 8;
    static final long OUTAGE_FROM_MS = 2_000, OUTAGE_TO_MS = 4_000;

    static final class Overloaded extends RuntimeException {
        Overloaded() { super("503", null, false, false); }
    }

    static final class Failed extends RuntimeException {
        Failed() { super("500", null, false, false); }
    }

    /** Downstream co nang luc huu han. */
    static final class Downstream {
        final Semaphore slots = new Semaphore(20);
        final long start;
        final LongAdder attempts = new LongAdder();
        final LongAdder attemptsDuringOutage = new LongAdder();
        final LongAdder rejected = new LongAdder();

        Downstream(long start) { this.start = start; }

        String call() {
            long t = elapsedMs(start);
            boolean outage = t >= OUTAGE_FROM_MS && t < OUTAGE_TO_MS;
            attempts.increment();
            if (outage) attemptsDuringOutage.increment();
            if (!slots.tryAcquire()) {
                rejected.increment();
                throw new Overloaded();
            }
            try {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(outage ? 100 : 10));
                if (outage) throw new Failed();
                return "ok";
            } finally {
                slots.release();
            }
        }
    }

    enum Policy { NO_RETRY, RETRY_3_IMMEDIATE, RETRY_3_BACKOFF_JITTER, BACKOFF_JITTER_PLUS_BREAKER }

    record Result(Policy policy, long requests, double amplification, long attemptsDuringOutage,
                  String successOverall, String successDuringOutage, String successFirst500msAfter,
                  long p99Ms) {}

    static Result run(Policy policy) throws InterruptedException {
        long start = System.nanoTime();
        var down = new Downstream(start);
        Supplier<String> call = down::call;

        if (policy != Policy.NO_RETRY) {
            RetryConfig.Builder<Object> rc = RetryConfig.custom().maxAttempts(4)    // 1 lan + 3 retry
                    .ignoreExceptions(CallNotPermittedException.class);
            if (policy == Policy.RETRY_3_IMMEDIATE) {
                rc.waitDuration(Duration.ofMillis(1));
            } else {
                rc.intervalFunction(IntervalFunction.ofExponentialRandomBackoff(Duration.ofMillis(100), 2.0, 0.5));
            }
            Supplier<String> inner = call;
            if (policy == Policy.BACKOFF_JITTER_PLUS_BREAKER) {
                var cb = CircuitBreaker.of("downstream", CircuitBreakerConfig.custom()
                        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(50).minimumNumberOfCalls(50).failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofMillis(500))
                        .permittedNumberOfCallsInHalfOpenState(10).build());
                inner = CircuitBreaker.decorateSupplier(cb, call);
            }
            call = Retry.decorateSupplier(Retry.of("downstream", rc.build()), inner);
        }

        var latencies = new ConcurrentLinkedQueue<long[]>();          // {arrivalMs, latencyMs, ok}
        long intervalNs = 1_000_000_000L / RPS;
        int total = RPS * SECONDS;
        Supplier<String> finalCall = call;
        try (var ex = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < total; i++) {
                long due = start + i * intervalNs;
                long wait = due - System.nanoTime();
                if (wait > 0) LockSupport.parkNanos(wait);
                long arrival = elapsedMs(start);
                ex.submit(() -> {
                    long t0 = System.nanoTime();
                    boolean ok;
                    try {
                        finalCall.get();
                        ok = true;
                    } catch (RuntimeException e) {
                        ok = false;
                    }
                    long lat = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                    if (lat > 1_000) ok = false;                     // qua deadline client: coi nhu that bai
                    latencies.add(new long[]{arrival, lat, ok ? 1 : 0});
                });
            }
        }
        long[] lat = latencies.stream().mapToLong(a -> a[1]).sorted().toArray();
        return new Result(policy, total, (double) down.attempts.sum() / total, down.attemptsDuringOutage.sum(),
                rate(latencies, 0, Long.MAX_VALUE),
                rate(latencies, OUTAGE_FROM_MS, OUTAGE_TO_MS),
                rate(latencies, OUTAGE_TO_MS, OUTAGE_TO_MS + 500),
                lat[(int) Math.ceil(0.99 * lat.length) - 1]);
    }

    static String rate(ConcurrentLinkedQueue<long[]> xs, long from, long to) {
        long n = 0, ok = 0;
        for (long[] x : xs) {
            if (x[0] >= from && x[0] < to) { n++; ok += x[2]; }
        }
        return String.format("%.1f%%", 100.0 * ok / Math.max(1, n));
    }

    static long elapsedMs(long start) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }

    public static void main(String[] args) throws Exception {
        System.out.printf("downstream 20 slot x 10 ms (2.000 req/s), su co %d-%d ms; client %d req/s x %d s, deadline 1 s%n",
                OUTAGE_FROM_MS, OUTAGE_TO_MS, RPS, SECONDS);
        run(Policy.NO_RETRY);                                        // warm-up
        Policy[] ps = args.length > 0 ? Arrays.stream(args).map(Policy::valueOf).toArray(Policy[]::new) : Policy.values();
        for (Policy p : ps) {
            System.out.println(run(p));
        }
    }
}
