package com.prep.kafka.resilience;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Token Bucket Rate Limiter phia Ingestion API:
 * - Bao ve Kafka Producer khoi bi tran bo nho dem (BufferExhaustedException) khi gap dot tai dot bien (burst)
 * - Tra ve HTTP 429 Too Many Requests khi tenant vuot quota
 */
@Component
public class ProducerRateLimiter {

    public static class Bucket {
        private final long capacity;
        private final double refillTokensPerMs;
        private double tokens;
        private long lastRefillTimestamp;

        public Bucket(long capacity, double refillTokensPerSecond) {
            this.capacity = capacity;
            this.refillTokensPerMs = refillTokensPerSecond / 1000.0;
            this.tokens = capacity;
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume(int requiredTokens) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTimestamp;
            tokens = Math.min(capacity, tokens + elapsed * refillTokensPerMs);
            lastRefillTimestamp = now;

            if (tokens >= requiredTokens) {
                tokens -= requiredTokens;
                return true;
            }
            return false;
        }

        public synchronized double getAvailableTokens() {
            return tokens;
        }
    }

    private final ConcurrentHashMap<String, Bucket> tenantBuckets = new ConcurrentHashMap<>();
    private final long defaultCapacity;
    private final double defaultRefillRate;

    public ProducerRateLimiter() {
        this(200, 100.0); // Mac dinh cho phep burst 200 tokens, hoi phuc 100 tokens/s
    }

    public ProducerRateLimiter(long capacity, double refillTokensPerSecond) {
        this.defaultCapacity = capacity;
        this.defaultRefillRate = refillTokensPerSecond;
    }

    public boolean tryAcquire(String tenantId) {
        return tryAcquire(tenantId, 1);
    }

    public boolean tryAcquire(String tenantId, int tokens) {
        Bucket bucket = tenantBuckets.computeIfAbsent(tenantId,
                id -> new Bucket(defaultCapacity, defaultRefillRate));
        return bucket.tryConsume(tokens);
    }

    public void reset() {
        tenantBuckets.clear();
    }
}
