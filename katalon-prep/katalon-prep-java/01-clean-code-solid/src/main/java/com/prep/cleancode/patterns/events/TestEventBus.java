package com.prep.cleancode.patterns.events;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ============================================================================
 * OBSERVER — event bus cho test run.
 * ============================================================================
 *
 * <p>Checklist goc cua ban ghi: <i>"Observer (cho he thong ban event thong bao ket qua test)"</i>.
 * Dung. Nhung Observer viet nai ngay se sinh ra 3 bug kinh dien, va day la 3 thu interviewer dao:
 *
 * <ol>
 *   <li><b>Mot listener nem lam chet ca test run.</b> Webhook cua khach 500 -> test run bao fail,
 *       du test that su pass. Su co nay tot nghiep tu "loi tich hop" thanh "mat du lieu ket qua".
 *   <li><b>Mot listener cham lam nghen tat ca.</b> Listener goi HTTP dong bo 2 giay x 10.000 event
 *       = test run cham gap nhieu lan.
 *   <li><b>ConcurrentModificationException</b> khi listener tu unregister minh trong luc dang
 *       duoc goi (vi du: one-shot listener). Rat kho debug vi chi xay ra o mot so run.
 * </ol>
 *
 * <p>Class nay xu ly ca ba, va co test cho ca ba.
 */
public final class TestEventBus {

    /** Su kien — sealed de listener switch exhaustive, khong can default. */
    public sealed interface TestEvent {
        String runId();

        record RunStarted(String runId, int totalTests) implements TestEvent {}

        record StepPassed(String runId, String stepName, Duration duration) implements TestEvent {}

        record StepFailed(String runId, String stepName, String reason) implements TestEvent {}

        record StepFlaky(String runId, String stepName, int attempts) implements TestEvent {}

        record RunFinished(String runId, int passed, int failed, int flaky) implements TestEvent {}
    }

    /**
     * Listener. Hop dong: <b>khong duoc nem</b> va <b>phai nhanh</b>.
     *
     * <p>Nhung hop dong khong tu cuong che duoc — bus phai cuong che ho (giong
     * {@code HealthAggregator.guarded()}). Do la ly do co {@link #publish}.
     */
    public interface TestEventListener {
        void onEvent(TestEvent event);

        /** Ten de log duoc listener nao gay loi. Khong co ten thi khong debug duoc. */
        default String name() {
            return getClass().getSimpleName();
        }
    }

    /**
     * CopyOnWriteArrayList: giai quyet bug #3.
     *
     * <p>Vi sao khong dung {@code ArrayList} + {@code synchronized}: khi dang duyet de goi listener
     * ma mot listener goi {@code unregister(this)}, {@code ArrayList} se nem
     * {@code ConcurrentModificationException}. {@code CopyOnWriteArrayList} duyet tren snapshot nen
     * an toan. Doi lai: moi lan register/unregister copy ca array — chi phap nay dung DUNG khi
     * "doc rat nhieu, ghi rat it", va dang ky listener dung la vay.
     */
    private final List<TestEventListener> listeners = new CopyOnWriteArrayList<>();

    /** Loi cua listener duoc GIU LAI, khong nem ra va cung khong bi nuot mat. */
    private final List<ListenerFailure> failures = new CopyOnWriteArrayList<>();

    private final AtomicInteger publishedCount = new AtomicInteger();

    public record ListenerFailure(String listenerName, TestEvent event, Throwable cause) {}

    public void register(TestEventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        listeners.add(listener);
    }

    public void unregister(TestEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Phat event tới moi listener. <b>KHONG BAO GIO nem</b>, ke ca khi listener nem.
     *
     * <p>Giai quyet bug #1: loi cua listener duoc thu vao {@link #failures} de bao cao rieng, thay
     * vi lan sang ket qua test. Test pass thi phai bao pass — bat ke webhook cua khach co song hay
     * khong. Do la ranh gioi trach nhiem, va la thu ma mot Lead phai nhin ra.
     */
    public void publish(TestEvent event) {
        publishedCount.incrementAndGet();
        for (TestEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException ex) {
                failures.add(new ListenerFailure(listener.name(), event, ex));
            }
        }
    }

    public List<ListenerFailure> failures() {
        return List.copyOf(failures);
    }

    public int listenerCount() {
        return listeners.size();
    }

    public int publishedCount() {
        return publishedCount.get();
    }

    // ------------------------------------------------------------------
    // Cac listener mau
    // ------------------------------------------------------------------

    /** Gom so lieu — dung pattern matching exhaustive tren sealed interface. */
    public static final class MetricsListener implements TestEventListener {
        private int passed;
        private int failed;
        private int flaky;
        private Duration totalDuration = Duration.ZERO;

        @Override
        public void onEvent(TestEvent event) {
            switch (event) {
                case TestEvent.RunStarted ignored -> reset();
                case TestEvent.StepPassed(var ignored, var ignored2, Duration d) -> {
                    passed++;
                    totalDuration = totalDuration.plus(d);
                }
                case TestEvent.StepFailed ignored -> failed++;
                case TestEvent.StepFlaky ignored -> flaky++;
                case TestEvent.RunFinished ignored -> {
                    // khong lam gi — so lieu da duoc gom dan
                }
            }
        }

        private void reset() {
            passed = 0;
            failed = 0;
            flaky = 0;
            totalDuration = Duration.ZERO;
        }

        public int passed() {
            return passed;
        }

        public int failed() {
            return failed;
        }

        public int flaky() {
            return flaky;
        }

        public Duration totalDuration() {
            return totalDuration;
        }
    }

    /**
     * Listener BUFFER thay vi goi HTTP ngay — giai quyet bug #2.
     *
     * <p>Thay vi mot HTTP call cho moi event (10.000 event = 10.000 request), gom vao buffer va chi
     * flush khi day hoac khi run ket thuc. Day chinh la <b>batching</b>, ky thuat co ban nhat de
     * giam tai downstream, va la thu ban se dung lai trong bai system design TrueTest.
     */
    public static final class BatchingWebhookListener implements TestEventListener {
        private final int batchSize;
        private final List<TestEvent> buffer = new ArrayList<>();
        private final List<List<TestEvent>> flushedBatches = new ArrayList<>();

        public BatchingWebhookListener(int batchSize) {
            if (batchSize < 1) {
                throw new IllegalArgumentException("batchSize must be >= 1");
            }
            this.batchSize = batchSize;
        }

        @Override
        public void onEvent(TestEvent event) {
            buffer.add(event);
            // Flush khi day HOAC khi run ket thuc (neu khong, batch cuoi se bi mat).
            if (buffer.size() >= batchSize || event instanceof TestEvent.RunFinished) {
                flush();
            }
        }

        private void flush() {
            if (buffer.isEmpty()) {
                return;
            }
            flushedBatches.add(List.copyOf(buffer));
            buffer.clear();
        }

        public List<List<TestEvent>> flushedBatches() {
            return List.copyOf(flushedBatches);
        }

        /** Event con trong buffer chua flush — phai bang 0 sau RunFinished. */
        public int pending() {
            return buffer.size();
        }
    }

    /** Listener chi quan tam mot loai event — vi du gui alert khi fail. */
    public static final class FailureAlertListener implements TestEventListener {
        private final List<String> alerts = new ArrayList<>();

        @Override
        public void onEvent(TestEvent event) {
            // Pattern matching voi instanceof — gon hon switch khi chi can 1 nhanh.
            if (event instanceof TestEvent.StepFailed(var runId, var step, var reason)) {
                alerts.add("%s/%s: %s".formatted(runId, step, reason));
            }
        }

        public List<String> alerts() {
            return List.copyOf(alerts);
        }
    }
}
