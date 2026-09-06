package com.prep.refresher;

import com.prep.refresher.TestResult.Failed;
import com.prep.refresher.TestResult.Flaky;
import com.prep.refresher.TestResult.Passed;
import com.prep.refresher.TestResult.Skipped;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cac ky thuat Java 21 ban se PHAI viet tay trong vong live-coding.
 * Doc ky comment - moi method day 1 bay thuong gap.
 */
public final class ResultReporter {

    private ResultReporter() {}

    /**
     * PATTERN MATCHING FOR SWITCH + RECORD DECONSTRUCTION.
     *
     * <p>Chu y 3 diem se bi hoi:
     * <ol>
     *   <li>KHONG co `default` - vi TestResult la sealed, compiler tu biet da phu het. Thu them
     *       1 record moi vao TestResult ma khong sua day -> compile error. Do la tinh nang, khong
     *       phai bat tien.
     *   <li>`Failed(var name, var msg, var trace)` la RECORD PATTERN - destructure truc tiep,
     *       khong can goi getter.
     *   <li>`when` la GUARDED PATTERN - loc them dieu kien tren cung 1 nhanh type.
     * </ol>
     */
    public static String describe(TestResult result) {
        return switch (result) {
            case Passed(String name, Duration d) when d.toMillis() > 5_000 ->
                    "%s PASSED but SLOW (%d ms)".formatted(name, d.toMillis());
            case Passed(String name, Duration d) -> "%s passed in %d ms".formatted(name, d.toMillis());
            case Flaky(String name, int attempts, var d) ->
                    "%s FLAKY: passed on attempt %d (%d ms)".formatted(name, attempts, d.toMillis());
            case Failed(String name, String msg, var trace) ->
                    "%s FAILED: %s (%d stack lines)".formatted(name, msg, trace.size());
            case Skipped(String name, String reason) -> "%s skipped: %s".formatted(name, reason);
        };
    }

    /**
     * BAY SO 1 - Collectors.toMap NEM IllegalStateException khi trung key.
     *
     * <p>Day chinh la bug tiem an trong code RCI cua ban:
     * {@code services.stream().collect(Collectors.toMap(s -> s, this::getDoCheckFuture))}
     * - neu `services` co phan tu trung, no NEM luc runtime, khong ai biet truoc.
     *
     * <p>Luon truyen merge function thu 3 khi khong CHAC 100% key la unique.
     */
    public static Map<String, TestResult> indexByNameLastWins(List<TestResult> results) {
        return results.stream()
                .collect(Collectors.toMap(TestResult::testName, Function.identity(), (first, second) -> second));
    }

    /**
     * BAY SO 2 - groupingBy tra ve Map co the KHONG chua key ban mong doi.
     * Neu khong co ket qua Failed nao, key "FAILED" khong ton tai (khong phai list rong).
     * -> phia doc phai dung getOrDefault, dung `.get(...)` roi .size() se NPE.
     */
    public static Map<String, List<TestResult>> groupByOutcome(List<TestResult> results) {
        return results.stream().collect(Collectors.groupingBy(ResultReporter::outcomeOf));
    }

    private static String outcomeOf(TestResult r) {
        // switch tren type, dang khong destructure - ngan hon khi khong can field.
        return switch (r) {
            case Passed ignored -> "PASSED";
            case Flaky ignored -> "FLAKY";
            case Failed ignored -> "FAILED";
            case Skipped ignored -> "SKIPPED";
        };
    }

    /**
     * BAY SO 3 - orElse vs orElseGet.
     * `orElse(expensive())` LUON goi expensive() ngay ca khi Optional CO gia tri (vi doi so phai
     * duoc eval truoc khi goi method). `orElseGet(() -> expensive())` chi goi khi rong.
     * Sonar co rule rieng cho cai nay.
     *
     * <p>O day dung `orElse(Duration.ZERO)` la DUNG: ZERO la mot hang so `static final`, khong ton
     * chi phi tao. Dung orElseGet cho hang so chi lam code dai hon vo ich.
     *
     * <p>Ghi chu them: `Duration.ZERO` la FIELD chu khong phai method, nen KHONG viet duoc
     * `Duration::ZERO` - method reference chi tro toi method. Compiler se bao
     * "invalid method reference". Day la loi rat de mac khi lau khong viet Java.
     */
    public static Duration slowest(List<TestResult> results) {
        return results.stream()
                .map(ResultReporter::durationOf)
                .flatMap(Optional::stream) // Optional::stream - bo cac Optional rong, Java 9+
                .max(Duration::compareTo)
                .orElse(Duration.ZERO);
    }

    private static Optional<Duration> durationOf(TestResult r) {
        return switch (r) {
            case Passed(var ignored, Duration d) -> Optional.of(d);
            case Flaky(var ignored, var ignored2, Duration d) -> Optional.of(d);
            case Failed ignored -> Optional.empty();
            case Skipped ignored -> Optional.empty();
        };
    }

    /**
     * Flakiness rate - so lieu Katalon thuc su quan tam.
     * Chu y: chia cho tong so test CHAY (khong tinh skipped), khong phai tong so test.
     */
    public static double flakinessRate(List<TestResult> results) {
        long executed = results.stream().filter(r -> !(r instanceof Skipped)).count();
        if (executed == 0) {
            return 0.0; // tra 0 thay vi NaN - NaN lam vo moi dashboard phia sau
        }
        long flaky = results.stream().filter(Flaky.class::isInstance).count();
        return (double) flaky / executed;
    }

    /**
     * TEXT BLOCK - viet report nhieu dong khong can \n va noi chuoi.
     * `\` cuoi dong = khong xuong dong (line continuation). `.stripIndent()` da tu dong.
     */
    public static String summary(List<TestResult> results) {
        Map<String, List<TestResult>> byOutcome = groupByOutcome(results);
        return """
               Test run summary
               ================
               passed  : %d
               flaky   : %d
               failed  : %d
               skipped : %d
               slowest : %d ms
               """
                .formatted(
                        byOutcome.getOrDefault("PASSED", List.of()).size(),
                        byOutcome.getOrDefault("FLAKY", List.of()).size(),
                        byOutcome.getOrDefault("FAILED", List.of()).size(),
                        byOutcome.getOrDefault("SKIPPED", List.of()).size(),
                        slowest(results).toMillis());
    }
}
