package com.prep.perf.demo;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/** Gom latency (nano giay) tu nhieu thread roi tinh p50/p95/p99/max. Do phan vi, khong do trung binh. */
final class Lat {

    private final ConcurrentLinkedQueue<Long> samples = new ConcurrentLinkedQueue<>();

    void add(long nanos) {
        samples.add(nanos);
    }

    int count() {
        return samples.size();
    }

    long pMs(double p) {
        long[] a = samples.stream().mapToLong(Long::longValue).toArray();
        if (a.length == 0) {
            return 0;
        }
        Arrays.sort(a);
        int idx = (int) Math.ceil(p / 100.0 * a.length) - 1;
        return TimeUnit.NANOSECONDS.toMillis(a[Math.max(0, Math.min(idx, a.length - 1))]);
    }

    long countOver(long millis) {
        long limit = TimeUnit.MILLISECONDS.toNanos(millis);
        return samples.stream().filter(x -> x > limit).count();
    }

    String summary() {
        return String.format("p50=%dms p95=%dms p99=%dms max=%dms", pMs(50), pMs(95), pMs(99), pMs(100));
    }

    static void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
