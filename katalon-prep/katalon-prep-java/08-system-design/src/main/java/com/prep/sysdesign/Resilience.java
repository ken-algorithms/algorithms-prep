package com.prep.sysdesign;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * SYSTEM DESIGN — ba primitive xuat hien trong gan nhu moi bai design: rate limiter,
 * circuit breaker, va idempotency store.
 *
 * <p><b>Luu y thiet ke chung:</b> ca ba deu nhan {@link LongSupplier} lam nguon thoi gian thay vi
 * goi {@code System.currentTimeMillis()} truc tiep. Do la ly do chung test duoc ma khong can
 * {@code Thread.sleep}: test bơm thoi gian gia. Neu ban lam nguoc lai, test se cham va flaky —
 * day la mot trong nhung quyet dinh testability quan trong nhat khi viet code co yeu to thoi gian.
 */
public final class Resilience {

    private Resilience() {}

    // ==================================================================
    // TOKEN BUCKET RATE LIMITER
    // ==================================================================

    /**
     * Token bucket — quota theo tenant.
     *
     * <p><b>Vi sao token bucket ma khong phai fixed window?</b> Fixed window co bug "bien cua so":
     * gioi han 100 req/phut thi client co the gui 100 req luc 00:59 va 100 req luc 01:00 = 200 req
     * trong 2 giay. Token bucket khong co bien nen khong co bug do.
     *
     * <p><b>Vi sao token bucket ma khong phai leaky bucket?</b> Token bucket cho phep BURST (tich
     * luy token khi ranh roi dung mot lan) — dung voi hanh vi that cua CI: im lang ca dem roi
     * 500 test cung luc luc 9 gio sang. Leaky bucket lam phang tuyet doi, se lam CI cham vo ly.
     *
     * <p>Danh doi cua burst: downstream phai chiu duoc burst = capacity. Nen capacity la mot
     * quyet dinh ve DUNG LUONG, khong phai mot con so tuy y.
     */
    public static final class TokenBucket {

        private final long capacity;
        private final double refillPerMilli;
        private final LongSupplier clock;
        private double tokens;
        private long lastRefillAt;

        /**
         * @param capacity so token toi da (= burst toi da cho phep)
         * @param refillPerSecond so token nap lai moi giay (= throughput on dinh)
         * @param clock nguon thoi gian millis; truyen fake trong test
         */
        public TokenBucket(long capacity, double refillPerSecond, LongSupplier clock) {
            if (capacity < 1) {
                throw new IllegalArgumentException("capacity must be >= 1");
            }
            if (refillPerSecond <= 0) {
                throw new IllegalArgumentException("refillPerSecond must be > 0");
            }
            this.capacity = capacity;
            this.refillPerMilli = refillPerSecond / 1000.0;
            this.clock = clock;
            this.tokens = capacity; // bat dau day -> cho phep burst ngay
            this.lastRefillAt = clock.getAsLong();
        }

        /** @return true neu duoc phep, false neu vuot quota */
        public synchronized boolean tryAcquire(int permits) {
            if (permits < 1) {
                throw new IllegalArgumentException("permits must be >= 1");
            }
            refill();
            if (tokens >= permits) {
                tokens -= permits;
                return true;
            }
            return false;
        }

        public boolean tryAcquire() {
            return tryAcquire(1);
        }

        /**
         * Nap lai token theo thoi gian da troi qua — LAZY, khong can background thread.
         *
         * <p>Day la meo quan trong: mot rate limiter co timer nen dat gap nhieu lan va kho scale
         * (1 trieu tenant = 1 trieu timer). Tinh lai luc doc thi chi phi bang 0 khi khong ai goi.
         */
        private void refill() {
            long now = clock.getAsLong();
            long elapsed = now - lastRefillAt;
            if (elapsed <= 0) {
                return; // dong ho khong lui — chong bug khi NTP dieu chinh nguoc
            }
            tokens = Math.min(capacity, tokens + elapsed * refillPerMilli);
            lastRefillAt = now;
        }

        public synchronized long availableTokens() {
            refill();
            return (long) tokens;
        }
    }

    /** Rate limiter theo tenant — moi tenant mot bucket rieng. */
    public static final class PerTenantRateLimiter {
        private final Map<String, TokenBucket> buckets = new LinkedHashMap<>();
        private final long defaultCapacity;
        private final double defaultRefillPerSecond;
        private final LongSupplier clock;

        public PerTenantRateLimiter(long defaultCapacity, double defaultRefillPerSecond, LongSupplier clock) {
            this.defaultCapacity = defaultCapacity;
            this.defaultRefillPerSecond = defaultRefillPerSecond;
            this.clock = clock;
        }

        public boolean tryAcquire(String tenantId) {
            // computeIfAbsent: bucket duoc tao lazy. Trong production can eviction (Caffeine voi
            // expireAfterAccess) neu khong map se phinh vo han theo so tenant tung goi.
            return buckets
                    .computeIfAbsent(tenantId, k -> new TokenBucket(defaultCapacity, defaultRefillPerSecond, clock))
                    .tryAcquire();
        }

        public int trackedTenants() {
            return buckets.size();
        }
    }

    // ==================================================================
    // CIRCUIT BREAKER
    // ==================================================================

    public enum State {
        /** Binh thuong, cho request di qua. */
        CLOSED,
        /** Downstream dang chet, CHAN het request de no co thoi gian hoi phuc. */
        OPEN,
        /** Thu mot it request de xem downstream da song lai chua. */
        HALF_OPEN
    }

    /** Nem khi circuit dang OPEN — request bi chan truoc khi cham downstream. */
    public static final class CircuitOpenException extends RuntimeException {
        public CircuitOpenException(String message) {
            super(message);
        }
    }

    /**
     * Circuit breaker — may trang thai 3 pha.
     *
     * <p><b>Vi sao can:</b> khi downstream chet, retry lam moi thu TE HON — ban gui gap 3 luu luong
     * vao mot service dang qua tai, va giu thread cua chinh minh de cho timeout. Circuit breaker
     * "that bai nhanh" de bao ve CA HAI phia.
     *
     * <p><b>Vi sao can HALF_OPEN:</b> neu chi co CLOSED/OPEN thi khi het thoi gian cho, TOAN BO
     * luu luong dap vao downstream cung luc va giet no lan nua (thundering herd). HALF_OPEN chi cho
     * mot so request tham do di qua.
     *
     * <p><b>Trade-off:</b> circuit breaker se chan ca nhung request LE RA thanh cong (false
     * positive) trong thoi gian OPEN. Doi lai, downstream co co hoi hoi phuc. Nguong va thoi gian
     * cho la thu phai tune theo so lieu that, khong phai lay mac dinh.
     */
    public static final class CircuitBreaker {

        private final int failureThreshold;
        private final int successThresholdInHalfOpen;
        private final Duration openDuration;
        private final LongSupplier clock;

        private State state = State.CLOSED;
        private int consecutiveFailures;
        private int consecutiveSuccessesInHalfOpen;
        private long openedAt;
        private long rejectedCount;

        public CircuitBreaker(
                int failureThreshold,
                int successThresholdInHalfOpen,
                Duration openDuration,
                LongSupplier clock) {
            if (failureThreshold < 1 || successThresholdInHalfOpen < 1) {
                throw new IllegalArgumentException("thresholds must be >= 1");
            }
            if (openDuration == null || openDuration.isNegative() || openDuration.isZero()) {
                throw new IllegalArgumentException("openDuration must be positive");
            }
            this.failureThreshold = failureThreshold;
            this.successThresholdInHalfOpen = successThresholdInHalfOpen;
            this.openDuration = openDuration;
            this.clock = clock;
        }

        public synchronized <T> T call(Supplier<T> action) {
            transitionIfCoolDownElapsed();

            if (state == State.OPEN) {
                rejectedCount++;
                throw new CircuitOpenException("circuit is OPEN, failing fast without calling downstream");
            }

            try {
                T result = action.get();
                onSuccess();
                return result;
            } catch (RuntimeException ex) {
                onFailure();
                throw ex;
            }
        }

        /** Het thoi gian cho -> OPEN chuyen sang HALF_OPEN de tham do. */
        private void transitionIfCoolDownElapsed() {
            if (state == State.OPEN && clock.getAsLong() - openedAt >= openDuration.toMillis()) {
                state = State.HALF_OPEN;
                consecutiveSuccessesInHalfOpen = 0;
            }
        }

        private void onSuccess() {
            if (state == State.HALF_OPEN) {
                consecutiveSuccessesInHalfOpen++;
                if (consecutiveSuccessesInHalfOpen >= successThresholdInHalfOpen) {
                    state = State.CLOSED;
                    consecutiveFailures = 0;
                }
                return;
            }
            consecutiveFailures = 0;
        }

        private void onFailure() {
            // Mot loi duy nhat trong HALF_OPEN la du de mo lai — downstream ro rang chua san sang.
            if (state == State.HALF_OPEN) {
                open();
                return;
            }
            consecutiveFailures++;
            if (consecutiveFailures >= failureThreshold) {
                open();
            }
        }

        private void open() {
            state = State.OPEN;
            openedAt = clock.getAsLong();
            consecutiveSuccessesInHalfOpen = 0;
        }

        public synchronized State state() {
            transitionIfCoolDownElapsed();
            return state;
        }

        /** So request bi chan — metric phai co, neu khong ban khong biet breaker dang hoat dong. */
        public synchronized long rejectedCount() {
            return rejectedCount;
        }
    }

    // ==================================================================
    // IDEMPOTENCY STORE
    // ==================================================================

    /**
     * Chong xu ly trung — thanh phan bat buoc cua kien truc "at-least-once + idempotent consumer".
     *
     * <p>Kafka/SQS deu la at-least-once: mot message CO THE den hai lan (producer retry, consumer
     * rebalance, commit offset that bai). Exactly-once thuc su rat dat va phuc tap. Cach re va dung
     * la: chap nhan trung, roi lam cho viec xu ly LAP LAI KHONG GAY HAI.
     *
     * <p>Trong DB that, day la {@code INSERT ... ON CONFLICT (event_id) DO NOTHING} — xem Lab 03
     * cua module 05. Ban in-memory nay de test logic va de noi ro vai TTL.
     *
     * <p><b>Vi sao phai co TTL:</b> khong the giu event_id mai mai (het bo nho / het disk). TTL
     * phai LON HON thoi gian retry toi da cua producer. Neu TTL nho hon, mot retry muon se bi coi
     * la event moi va duoc xu ly lan hai — bug rat kho tim vi chi xay ra khi he thong dang co su co.
     */
    public static final class IdempotencyStore {

        private final Map<String, Long> seenAt = new LinkedHashMap<>();
        private final Duration ttl;
        private final LongSupplier clock;

        public IdempotencyStore(Duration ttl, LongSupplier clock) {
            if (ttl == null || ttl.isNegative() || ttl.isZero()) {
                throw new IllegalArgumentException("ttl must be positive");
            }
            this.ttl = ttl;
            this.clock = clock;
        }

        /**
         * @return true neu day la lan DAU tien thay eventId (nen xu ly);
         *     false neu da thay roi (nen bo qua)
         */
        public synchronized boolean markIfFirstTime(String eventId) {
            evictExpired();
            Long previous = seenAt.putIfAbsent(eventId, clock.getAsLong());
            return previous == null;
        }

        private void evictExpired() {
            long cutoff = clock.getAsLong() - ttl.toMillis();
            seenAt.entrySet().removeIf(entry -> entry.getValue() < cutoff);
        }

        public synchronized int size() {
            evictExpired();
            return seenAt.size();
        }
    }

    /**
     * Consumer at-least-once co idempotency — mo phong dung luong xu ly message.
     *
     * <p>Chu y {@code processedOrder}: no chung minh cac side effect chi xay ra MOT lan cho moi
     * eventId, ke ca khi message den nhieu lan.
     */
    public static final class IdempotentConsumer {
        private final IdempotencyStore store;
        private final Set<String> processedOrder = new LinkedHashSet<>();
        private int duplicatesSkipped;

        public IdempotentConsumer(IdempotencyStore store) {
            this.store = store;
        }

        public void consume(String eventId) {
            if (!store.markIfFirstTime(eventId)) {
                duplicatesSkipped++;
                return;
            }
            processedOrder.add(eventId);
        }

        public Set<String> processed() {
            return Set.copyOf(processedOrder);
        }

        public int duplicatesSkipped() {
            return duplicatesSkipped;
        }
    }
}
