package com.prep.cleancode.patterns.execution;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * ============================================================================
 * DECORATOR — bao cac quan tam cat ngang quanh viec chay mot buoc test.
 * ============================================================================
 *
 * <p>Chay mot buoc test can rat nhieu thu phu: do thoi gian, retry khi flaky, chup screenshot khi
 * fail, log, dem metric. Neu nhet het vao mot class thi no thanh 300 dong lam 6 viec (vi pham SRP,
 * va la chinh xac hinh dang cua class {@code HealthCheckContextStrategy} o package {@code before}).
 *
 * <p>Decorator cho phep <b>gan/thao tung moi quan tam doc lap</b> va <b>doi thu tu</b> — ma thu tu
 * la diem phong van hay dao nhat, xem {@link #ORDER_MATTERS}.
 */
public final class Executors {

    private Executors() {}

    /**
     * <b>THU TU DECORATOR LA MOT QUYET DINH THIET KE, KHONG PHAI CHI TIET.</b>
     *
     * <pre>
     * retry(timing(step))   -> moi LAN THU duoc do rieng. Metric = latency cua 1 attempt.
     *                          Tot khi ban muon biet "mot attempt binh thuong mat bao lau".
     * timing(retry(step))   -> do TONG ca qua trinh retry. Metric = latency nguoi dung cam nhan.
     *                          Tot khi ban do SLO tu goc nhin nguoi dung.
     * </pre>
     *
     * Ca hai deu "dung", nhung tra loi hai cau hoi khac nhau. Neu ban dat sai, dashboard p95 se noi
     * doi: retry(timing(...)) lam p95 trong DEP ra mot cach gia tao vi moi attemp ngan, trong khi
     * nguoi dung thuc su cho gap 3 lan.
     *
     * <p>Tuong tu voi screenshot: {@code retry(screenshot(step))} chup MOI lan fail (ton storage
     * nhung day du bang chung); {@code screenshot(retry(step))} chi chup khi da het retry (re hon,
     * nhung mat dau vet cua cac lan fail giua).
     */
    public static final String ORDER_MATTERS =
            "retry(timing(x)) do tung attempt; timing(retry(x)) do trai nghiem nguoi dung";

    public record TestStep(String name, Supplier<Boolean> action) {}

    /**
     * Ket qua chay mot buoc. {@code attempts} va {@code artifacts} la thu van hanh can, va la thu
     * ma phien ban "tra ve boolean" khong bao gio cung cap duoc.
     */
    public record StepResult(
            String stepName,
            boolean passed,
            int attempts,
            Duration duration,
            String failureReason,
            List<String> artifacts) {

        public StepResult {
            failureReason = failureReason == null ? "" : failureReason;
            artifacts = List.copyOf(artifacts);
        }

        public static StepResult pass(String name) {
            return new StepResult(name, true, 1, Duration.ZERO, "", List.of());
        }

        public static StepResult fail(String name, String reason) {
            return new StepResult(name, false, 1, Duration.ZERO, reason, List.of());
        }

        StepResult withAttempts(int value) {
            return new StepResult(stepName, passed, value, duration, failureReason, artifacts);
        }

        StepResult withDuration(Duration value) {
            return new StepResult(stepName, passed, attempts, value, failureReason, artifacts);
        }

        StepResult plusArtifact(String artifact) {
            var merged = new ArrayList<>(artifacts);
            merged.add(artifact);
            return new StepResult(stepName, passed, attempts, duration, failureReason, merged);
        }
    }

    /** Component interface — moi decorator vua implement vua chua mot cai khac. */
    public interface StepExecutor {
        StepResult execute(TestStep step);
    }

    /**
     * Executor goc: chi chay action, khong them gi. Bat exception thanh StepResult.fail — quy tac
     * "errors as values" giong {@code HealthAggregator}.
     */
    public static final class PlainExecutor implements StepExecutor {
        @Override
        public StepResult execute(TestStep step) {
            try {
                return Boolean.TRUE.equals(step.action().get())
                        ? StepResult.pass(step.name())
                        : StepResult.fail(step.name(), "assertion failed");
            } catch (RuntimeException ex) {
                String message = ex.getMessage() == null ? "" : ": " + ex.getMessage();
                return StepResult.fail(step.name(), ex.getClass().getSimpleName() + message);
            }
        }
    }

    /**
     * Decorator retry.
     *
     * <p>Diem quan trong ve VAN HANH: khi mot buoc pass sau khi retry, no KHONG phai "pass". No la
     * <b>flaky</b>, va phai duoc bao cao la flaky. Ban {@code attempts > 1 && passed} chinh la tin
     * hieu do. Neu che di (bao pass) thi test suite se muc dan ma khong ai biet — dung cai benh ma
     * TrueTest cua Katalon sinh ra de chua.
     */
    public static final class RetryingExecutor implements StepExecutor {
        private final StepExecutor delegate;
        private final int maxAttempts;

        public RetryingExecutor(StepExecutor delegate, int maxAttempts) {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("maxAttempts must be >= 1, got " + maxAttempts);
            }
            this.delegate = delegate;
            this.maxAttempts = maxAttempts;
        }

        @Override
        public StepResult execute(TestStep step) {
            StepResult last = null;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                last = delegate.execute(step);
                if (last.passed()) {
                    return last.withAttempts(attempt);
                }
            }
            return last.withAttempts(maxAttempts);
        }
    }

    /** Decorator do thoi gian. Dung nanoTime, KHONG dung currentTimeMillis (co the nhay khi NTP dieu chinh). */
    public static final class TimingExecutor implements StepExecutor {
        private final StepExecutor delegate;

        public TimingExecutor(StepExecutor delegate) {
            this.delegate = delegate;
        }

        @Override
        public StepResult execute(TestStep step) {
            long start = System.nanoTime();
            StepResult result = delegate.execute(step);
            return result.withDuration(Duration.ofNanos(System.nanoTime() - start));
        }
    }

    /** Decorator chup screenshot khi fail. Chi them artifact, khong doi ket qua pass/fail. */
    public static final class ScreenshotOnFailureExecutor implements StepExecutor {
        private final StepExecutor delegate;
        private final Supplier<String> screenshotTaker;

        public ScreenshotOnFailureExecutor(StepExecutor delegate, Supplier<String> screenshotTaker) {
            this.delegate = delegate;
            this.screenshotTaker = screenshotTaker;
        }

        @Override
        public StepResult execute(TestStep step) {
            StepResult result = delegate.execute(step);
            if (result.passed()) {
                return result;
            }
            try {
                return result.plusArtifact(screenshotTaker.get());
            } catch (RuntimeException ex) {
                // Chup screenshot LOI khong duoc bien mot test fail thanh mot exception khac.
                // Nguyen nhan goc phai duoc giu nguyen.
                return result.plusArtifact("screenshot-failed: " + ex.getClass().getSimpleName());
            }
        }
    }

    /**
     * Helper lap chuoi decorator theo thu tu doc tu ngoai vao trong.
     *
     * <p>{@code decorate(plain).with(timing).with(retry)} => {@code retry(timing(plain))}.
     * Viet kieu builder de doc xuoi, tranh doc nguoc tu trong ra nhu khi long constructor.
     */
    public static Builder decorate(StepExecutor core) {
        return new Builder(core);
    }

    public static final class Builder {
        private StepExecutor current;

        private Builder(StepExecutor core) {
            this.current = core;
        }

        public Builder with(java.util.function.UnaryOperator<StepExecutor> wrapper) {
            this.current = wrapper.apply(current);
            return this;
        }

        public StepExecutor build() {
            return current;
        }
    }
}
