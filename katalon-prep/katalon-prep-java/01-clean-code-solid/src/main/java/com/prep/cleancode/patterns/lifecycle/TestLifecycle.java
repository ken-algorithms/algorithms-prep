package com.prep.cleancode.patterns.lifecycle;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * TEMPLATE METHOD + BUILDER — vong doi mot test case.
 * ============================================================================
 *
 * <p><b>Template Method</b> giai quyet mot bug that: <b>teardown khong chay khi setup hoac execute
 * nem</b>. Hau qua o he thong test that: browser khong bi dong, container khong bi xoa, record
 * trong DB khong bi don -> ro ri tai nguyen dan, den luc thi CI het disk hoac het license Appium.
 *
 * <p>Ban khong the tin subclass se nho viet {@code try/finally}. Nen lop cha <b>giu quyen dieu
 * phoi</b> ({@link #run()} la {@code final}), subclass chi dien vao cac hook. Do chinh la Template
 * Method: thu tu buoc la bat bien cua he thong, khong phai lua chon cua nguoi viet test.
 */
public abstract class TestLifecycle {

    /** Ghi nhan tung giai doan de test duoc va de debug duoc. */
    public record Phase(String name, boolean succeeded, String error) {
        public Phase {
            error = error == null ? "" : error;
        }
    }

    public record Outcome(String testName, boolean passed, List<Phase> phases, Duration duration) {
        public Outcome {
            phases = List.copyOf(phases);
        }

        public boolean teardownRan() {
            return phases.stream().anyMatch(p -> "teardown".equals(p.name()));
        }

        public List<String> errors() {
            return phases.stream().filter(p -> !p.succeeded()).map(Phase::error).toList();
        }
    }

    protected abstract String testName();

    /** Chuan bi: mo browser, tao du lieu. Duoc phep nem. */
    protected abstract void setUp();

    /** Than test. Duoc phep nem. */
    protected abstract void execute();

    /**
     * Don dep. <b>LUON duoc goi</b>, ke ca khi setUp hoac execute nem.
     *
     * <p>Chu y: teardown cung co the nem, va loi cua no KHONG duoc che mat loi goc — day la bay
     * "exception masking" kinh dien, xem {@link #run()}.
     */
    protected abstract void tearDown();

    /**
     * TEMPLATE METHOD — {@code final} de subclass khong the doi thu tu hay bo qua teardown.
     *
     * <p>Ba dieu quan trong:
     * <ol>
     *   <li>{@code finally} bao dam teardown chay ke ca khi setUp nem.
     *   <li>Neu setUp nem thi <b>khong chay</b> execute (chay tiep tren trang thai nua voi la vo nghia,
     *       va sinh ra loi thu hai che mat loi thu nhat).
     *   <li>Loi cua teardown duoc ghi RIENG, khong ghi de loi goc. Neu chi
     *       {@code throw} trong finally thi loi goc bien mat va ban debug sai cho hang gio.
     * </ol>
     */
    public final Outcome run() {
        var phases = new ArrayList<Phase>(3);
        long start = System.nanoTime();
        boolean setUpOk = false;

        try {
            try {
                setUp();
                phases.add(new Phase("setUp", true, ""));
                setUpOk = true;
            } catch (RuntimeException ex) {
                phases.add(new Phase("setUp", false, describe(ex)));
            }

            if (setUpOk) {
                try {
                    execute();
                    phases.add(new Phase("execute", true, ""));
                } catch (RuntimeException ex) {
                    phases.add(new Phase("execute", false, describe(ex)));
                }
            }
        } finally {
            // LUON chay, va loi cua no khong duoc che loi truoc do.
            try {
                tearDown();
                phases.add(new Phase("teardown", true, ""));
            } catch (RuntimeException ex) {
                phases.add(new Phase("teardown", false, describe(ex)));
            }
        }

        boolean passed = phases.stream()
                .filter(p -> !"teardown".equals(p.name()))
                .allMatch(Phase::succeeded)
                && setUpOk;

        return new Outcome(
                testName(), passed, phases, Duration.ofNanos(System.nanoTime() - start));
    }

    private static String describe(Throwable ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank()
                ? ex.getClass().getSimpleName()
                : ex.getClass().getSimpleName() + ": " + message;
    }

    // ==================================================================
    // BUILDER — dung TestCase co validate, thay cho constructor 8 tham so
    // ==================================================================

    /**
     * <b>BUILDER</b> giai quyet hai van de:
     *
     * <ol>
     *   <li>Constructor nhieu tham so cung kieu: {@code new TestCase(name, suite, owner, tag, env)}
     *       — 5 String, doi cho hai cai la compiler khong bao gi, bug im lang. Sonar co rule
     *       {@code java:S107} cho so tham so, nhung van de that la <b>khong doc duoc</b>.
     *   <li>Doi tuong nua voi: neu dung setter, object ton tai o trang thai chua hop le giua cac lan
     *       set. Builder dam bao <b>validate mot lan trong {@code build()}</b>, va san pham la
     *       immutable.
     * </ol>
     *
     * <p>Chu y: validate phai nam trong {@code build()}, KHONG nam trong tung setter — vi rang buoc
     * lien-truong (vi du "retries > 0 thi phai co timeout") chi kiem tra duoc khi da co du thong tin.
     */
    public record TestCase(
            String name,
            String suite,
            Platform platform,
            Duration timeout,
            int maxRetries,
            Map<String, String> tags) {

        public enum Platform {
            WEB,
            ANDROID,
            IOS,
            API
        }

        public TestCase {
            tags = Map.copyOf(tags);
        }

        public static Builder builder(String name) {
            return new Builder(name);
        }

        public static final class Builder {
            private final String name;
            private String suite = "default";
            private Platform platform = Platform.WEB;
            private Duration timeout = Duration.ofSeconds(30);
            private int maxRetries;
            private final Map<String, String> tags = new LinkedHashMap<>();

            private Builder(String name) {
                this.name = name;
            }

            public Builder suite(String value) {
                this.suite = value;
                return this;
            }

            public Builder platform(Platform value) {
                this.platform = value;
                return this;
            }

            public Builder timeout(Duration value) {
                this.timeout = value;
                return this;
            }

            public Builder maxRetries(int value) {
                this.maxRetries = value;
                return this;
            }

            public Builder tag(String key, String value) {
                this.tags.put(key, value);
                return this;
            }

            /** Toan bo validate o day — mot cho duy nhat, ke ca rang buoc lien-truong. */
            public TestCase build() {
                var problems = new ArrayList<String>();
                if (name == null || name.isBlank()) {
                    problems.add("name must not be blank");
                }
                if (timeout == null || timeout.isZero() || timeout.isNegative()) {
                    problems.add("timeout must be positive, got " + timeout);
                }
                if (maxRetries < 0) {
                    problems.add("maxRetries must be >= 0, got " + maxRetries);
                }
                // Rang buoc LIEN-TRUONG: chi kiem tra duoc khi da co ca hai truong.
                if (maxRetries > 0 && timeout != null && timeout.toSeconds() > 300) {
                    problems.add(
                            "retrying a test with timeout > 300s can exceed the run budget; "
                                    + "lower the timeout or drop retries");
                }
                if (platform == Platform.API && !tags.isEmpty() && tags.containsKey("browser")) {
                    problems.add("API test must not declare a 'browser' tag");
                }

                // Bao CA CUM loi mot lan, khong bao tung cai roi bat nguoi dung sua 5 vong.
                if (!problems.isEmpty()) {
                    throw new IllegalStateException(
                            "invalid TestCase '%s': %s".formatted(name, String.join("; ", problems)));
                }
                return new TestCase(name, suite, platform, timeout, maxRetries, tags);
            }
        }
    }
}
