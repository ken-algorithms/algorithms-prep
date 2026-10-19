package com.prep.onbai.ratelimit;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Token bucket trong bo nho, theo tung key. Cho phep burst toi capacity, trung binh khong qua refillPerSecond.
 * ConcurrentHashMap.compute khoa dung mot bin cua key do -> doc-tinh-ghi nguyen tu, key khac khong cho nhau.
 * Dong ho tiem vao (nanoTime) -> test tat dinh, khong sleep.
 */
public final class TokenBucket {

    private record State(double tokens, long lastNanos) {}

    private final double capacity;
    private final double refillPerNano;
    private final LongSupplier nanoClock;
    private final ConcurrentHashMap<String, State> buckets = new ConcurrentHashMap<>();

    public TokenBucket(double capacity, double refillPerSecond, LongSupplier nanoClock) {
        this.capacity = capacity;
        this.refillPerNano = refillPerSecond / 1e9;
        this.nanoClock = nanoClock;
    }

    public boolean tryAcquire(String key) {
        boolean[] granted = new boolean[1];
        buckets.compute(key, (k, s) -> {
            long now = nanoClock.getAsLong();
            double tokens = s == null ? capacity : Math.min(capacity, s.tokens() + (now - s.lastNanos()) * refillPerNano);
            if (tokens >= 1) {
                tokens -= 1;
                granted[0] = true;
            }
            return new State(tokens, now);
        });
        return granted[0];
    }

    /** Map nay lon theo so key (moi IP, moi user...). That: dung Caffeine expireAfterAccess de key nguoi het han. */
    public int trackedKeys() {
        return buckets.size();
    }
}
