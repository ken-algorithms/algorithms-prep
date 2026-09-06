package com.prep.distributed;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongUnaryOperator;
import java.util.function.Supplier;

/**
 * RETRY + BACKOFF + JITTER — va vi sao thieu jitter la mot su co dang cho.
 *
 * <p><b>Bai toan thundering herd:</b> 1.000 worker cung goi mot service. Service do restart. Ca
 * 1.000 worker cung nhan loi TAI CUNG MOT THOI DIEM, roi cung retry sau dung 1 giay, roi 2 giay,
 * roi 4 giay... Chung dong bo hoa vinh vien va dap vao service dang hoi phuc theo tung dot 1.000
 * request. Service khong bao gio dung day duoc.
 *
 * <p><b>Jitter</b> pha su dong bo do bang cach ngau nhien hoa thoi gian cho. Voi <i>full jitter</i>
 * ({@code random(0, base*2^n)}), 1.000 worker se rai deu tren ca cua so thay vi don cuc.
 *
 * <p>Class nay dung {@link LongUnaryOperator} lam nguon ngau nhien de test DETERMINISTIC — day
 * la cung ky thuat "inject dependency thoi gian/ngau nhien" nhu {@code LongSupplier} o module 08.
 */
public final class Retries {

    private Retries() {}

    /** Ket qua mot lan thu — de test kiem tra duoc lich retry. */
    public record Attempt(int number, Duration delayBefore, boolean succeeded, String error) {}

    public record Outcome<T>(T value, boolean succeeded, List<Attempt> attempts) {
        public Outcome {
            attempts = List.copyOf(attempts);
        }

        public int attemptCount() {
            return attempts.size();
        }

        public Duration totalDelay() {
            return attempts.stream().map(Attempt::delayBefore).reduce(Duration.ZERO, Duration::plus);
        }
    }

    public enum Backoff {
        /** Cho co dinh. Don gian nhung DONG BO HOA cac client — tranh dung o he thong nhieu client. */
        FIXED,
        /** base * 2^n. Tot hon fixed, nhung van dong bo neu moi client cung cong thuc. */
        EXPONENTIAL,
        /** random(0, base * 2^n) — "full jitter" cua AWS. MAC DINH NEN DUNG. */
        EXPONENTIAL_FULL_JITTER,
        /** random(base, base*2^n) — "equal jitter": van co san sau toi thieu, bot cuc doan. */
        EXPONENTIAL_EQUAL_JITTER
    }

    public static final class Policy {
        private final int maxAttempts;
        private final Duration base;
        private final Duration cap;
        private final Backoff backoff;
        private final LongUnaryOperator random; // bound -> gia tri trong [0, bound)

        /**
         * @param cap TRAN cho thoi gian cho. Thieu cai nay thi lan thu 20 se cho 12 ngay —
         *     nghe buon cuoi nhung la bug that trong cac thu vien retry tu viet.
         * @param random nhan bound, tra so trong [0, bound). Test truyen ham deterministic.
         */
        public Policy(int maxAttempts, Duration base, Duration cap, Backoff backoff, LongUnaryOperator random) {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("maxAttempts must be >= 1");
            }
            if (base == null || base.isNegative()) {
                throw new IllegalArgumentException("base must be >= 0");
            }
            if (cap == null || cap.compareTo(base) < 0) {
                throw new IllegalArgumentException("cap must be >= base");
            }
            this.maxAttempts = maxAttempts;
            this.base = base;
            this.cap = cap;
            this.backoff = backoff;
            this.random = random;
        }

        /** Thoi gian cho TRUOC lan thu thu {@code attempt} (attempt bat dau tu 1). */
        public Duration delayBefore(int attempt) {
            if (attempt <= 1) {
                return Duration.ZERO; // lan dau khong cho
            }
            long exponent = attempt - 2L;
            long raw = base.toMillis() * (1L << Math.min(exponent, 30)); // chan tran khoi overflow
            long bounded = Math.min(raw, cap.toMillis());

            return switch (backoff) {
                case FIXED -> base;
                case EXPONENTIAL -> Duration.ofMillis(bounded);
                case EXPONENTIAL_FULL_JITTER -> Duration.ofMillis(random.applyAsLong(bounded + 1));
                case EXPONENTIAL_EQUAL_JITTER -> {
                    long half = bounded / 2;
                    yield Duration.ofMillis(half + random.applyAsLong(half + 1));
                }
            };
        }

        public int maxAttempts() {
            return maxAttempts;
        }
    }

    /**
     * Chay {@code action} voi retry. KHONG thuc su sleep — tra ve lich cho de test kiem tra.
     *
     * <p>Thiet ke nay co chu y: tach QUYET DINH (cho bao lau, thu may lan) khoi TAC DONG (sleep).
     * Nho vay logic retry test duoc trong vai micro-giay thay vi phai cho that.
     */
    public static <T> Outcome<T> execute(Policy policy, Supplier<T> action) {
        var attempts = new ArrayList<Attempt>(policy.maxAttempts());
        for (int i = 1; i <= policy.maxAttempts(); i++) {
            Duration delay = policy.delayBefore(i);
            try {
                T value = action.get();
                attempts.add(new Attempt(i, delay, true, ""));
                return new Outcome<>(value, true, attempts);
            } catch (RuntimeException ex) {
                attempts.add(new Attempt(i, delay, false, ex.getClass().getSimpleName()));
                if (!isRetryable(ex)) {
                    // KHONG retry loi cua chinh minh (400 Bad Request, validation...).
                    // Retry mot request sai se sai y het lan — chi ton tai nguyen va lam nhieu log.
                    return new Outcome<>(null, false, attempts);
                }
            }
        }
        return new Outcome<>(null, false, attempts);
    }

    /**
     * Chi retry loi TAM THOI. Day la quyet dinh quan trong nhat cua mot retry policy, va la thu
     * nhieu nguoi bo qua: retry mot loi 400/401/422 khong bao gio thanh cong.
     */
    public static boolean isRetryable(RuntimeException ex) {
        return !(ex instanceof NonRetryableException);
    }

    /** Loi cua PHIA GOI — retry vo nghia. */
    public static class NonRetryableException extends RuntimeException {
        public NonRetryableException(String message) {
            super(message);
        }
    }

    /** Loi tam thoi cua downstream — retry co y nghia. */
    public static class TransientException extends RuntimeException {
        public TransientException(String message) {
            super(message);
        }
    }
}
