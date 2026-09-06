package com.prep.refresher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.prep.refresher.TestResult.Failed;
import com.prep.refresher.TestResult.Flaky;
import com.prep.refresher.TestResult.Passed;
import com.prep.refresher.TestResult.Skipped;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * CACH DUNG FILE NAY:
 * 1. Doc ten test, TU DOAN ket qua truoc khi xem assert.
 * 2. Cho nao doan sai -> do la lo hong that sau 1 nam khong viet Java. Ghi lai.
 * 3. `mvn -pl 00-refresher-java21 test` de chay.
 */
class ResultReporterTest {

    private static final Duration FAST = Duration.ofMillis(120);
    private static final Duration SLOW = Duration.ofSeconds(9);
    // Tach hang so vi SonarLint rule java:S1192 (duplicated string literal).
    // Trong test, loi ich that la: doi ten fixture chi sua 1 cho.
    private static final String LOGIN = "login";
    private static final String NO_ENV = "no env";

    @Nested
    @DisplayName("Pattern matching for switch (Java 21)")
    class PatternMatching {

        @Test
        @DisplayName("guarded pattern: Passed cham thi vao nhanh SLOW truoc")
        void guardedPatternWinsWhenConditionMatches() {
            // Thu tu case QUAN TRONG: nhanh co `when` phai dat TRUOC nhanh khong dieu kien,
            // neu dao lai -> compile error "this case label is dominated by a preceding case label".
            assertEquals(
                    "login PASSED but SLOW (9000 ms)", ResultReporter.describe(new Passed(LOGIN, SLOW)));
            assertEquals("login passed in 120 ms", ResultReporter.describe(new Passed(LOGIN, FAST)));
        }

        @Test
        @DisplayName("record deconstruction lay field truc tiep, khong goi getter")
        void recordDeconstruction() {
            var failed = new Failed("checkout", "element not found", List.of("line1", "line2", "line3"));
            assertEquals("checkout FAILED: element not found (3 stack lines)", ResultReporter.describe(failed));
        }

        @Test
        @DisplayName("Flaky duoc bao cao rieng, khong lan vao Passed")
        void flakyIsDistinctFromPassed() {
            assertEquals(
                    "search FLAKY: passed on attempt 3 (120 ms)",
                    ResultReporter.describe(new Flaky("search", 3, FAST)));
        }
    }

    @Nested
    @DisplayName("Record invariants - record KHONG mien nhiem validate")
    class RecordInvariants {

        @Test
        @DisplayName("compact constructor chan message rong")
        void failedRequiresMessage() {
            var ex = assertThrows(
                    IllegalArgumentException.class, () -> new Failed("t", "  ", List.of()));
            assertTrue(ex.getMessage().contains("must carry a message"));
        }

        @Test
        @DisplayName("Flaky voi 1 attempt la vo nghia -> chan ngay tai constructor")
        void flakyNeedsAtLeastTwoAttempts() {
            assertThrows(IllegalArgumentException.class, () -> new Flaky("t", 1, FAST));
        }

        @Test
        @DisplayName("BAY: record immutable o reference, KHONG tu dong immutable o noi dung list")
        void defensiveCopyMatters() {
            var mutable = new ArrayList<String>();
            mutable.add("original");
            var failed = new Failed("t", "boom", mutable);

            mutable.add("sneaked in after construction");

            // Nho co List.copyOf trong compact constructor nen record KHONG bi anh huong.
            // Neu bo List.copyOf di, assert nay se thanh 2 -> day la lo hong immutability that.
            assertEquals(1, failed.stackTrace().size());

            // Va list ben trong record la unmodifiable.
            assertThrows(
                    UnsupportedOperationException.class, () -> failed.stackTrace().add("nope"));
        }

        @Test
        @DisplayName("record co equals/hashCode theo GIA TRI - dung duoc lam key cua Map/Set")
        void recordEqualityIsStructural() {
            var a = new Passed(LOGIN, FAST);
            var b = new Passed(LOGIN, FAST);

            // Record tu sinh equals/hashCode tu TAT CA component -> 2 instance khac nhau nhung
            // bang nhau ve gia tri. Day la ly do record thay the duoc DTO/value object thu cong.
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());

            // Dedupe hoat dong dung trong HashSet.
            assertEquals(1, new java.util.HashSet<>(List.of(a, b)).size());
        }

        @Test
        @DisplayName("BAY: Set.of/Map.of KHONG dedupe - chung nem IllegalArgumentException")
        void immutableFactoriesRejectDuplicatesInsteadOfDeduping() {
            var a = new Passed(LOGIN, FAST);
            var b = new Passed(LOGIN, FAST); // equals(a) == true

            // Rat de nham: List.of cho phep trung, nhung Set.of thi NEM.
            // Neu ban build Set.of(...) tu du lieu runtime (vd ten service) -> crash luc chay.
            var ex = assertThrows(IllegalArgumentException.class, () -> java.util.Set.of(a, b));
            assertTrue(ex.getMessage().contains("duplicate element"), ex.getMessage());

            // Map.of cung vay voi key trung.
            assertThrows(
                    IllegalArgumentException.class, () -> Map.of("k", 1, "k", 2));

            // Muon dedupe im lang -> dung HashSet, hoac stream().distinct().
            assertEquals(1, new java.util.HashSet<>(List.of(a, b)).size());
        }
    }

    @Nested
    @DisplayName("Stream/Collectors - cac bay hay lam vo production")
    class StreamTraps {

        @Test
        @DisplayName("BAY: Collectors.toMap KHONG co merge function se nem khi trung key")
        void toMapThrowsOnDuplicateKey() {
            List<TestResult> withDuplicate =
                    List.of(new Passed(LOGIN, FAST), new Failed(LOGIN, "boom", List.of()));

            // Bug tiem an dung dang nay ton tai trong code RCI cua ban.
            assertThrows(
                    IllegalStateException.class,
                    () -> withDuplicate.stream()
                            .collect(java.util.stream.Collectors.toMap(TestResult::testName, r -> r)));

            // Ban sua: co merge function -> last wins, khong nem.
            Map<String, TestResult> safe = ResultReporter.indexByNameLastWins(withDuplicate);
            assertEquals(1, safe.size());
            assertTrue(safe.get(LOGIN) instanceof Failed);
        }

        @Test
        @DisplayName("BAY: groupingBy khong tao key rong - phai dung getOrDefault")
        void groupingByOmitsEmptyBuckets() {
            var onlyPassed = List.<TestResult>of(new Passed("a", FAST));
            var grouped = ResultReporter.groupByOutcome(onlyPassed);

            assertEquals(1, grouped.size());
            assertTrue(grouped.containsKey("PASSED"));
            // Key "FAILED" KHONG ton tai - khong phai list rong. `.get("FAILED").size()` se NPE.
            assertTrue(grouped.get("FAILED") == null);
            assertEquals(0, grouped.getOrDefault("FAILED", List.of()).size());
        }

        @Test
        @DisplayName("Optional::stream de bo cac Optional rong khi flatMap")
        void slowestIgnoresResultsWithoutDuration() {
            List<TestResult> results = List.of(
                    new Passed("a", FAST),
                    new Failed("b", "boom", List.of()),
                    new Skipped("c", NO_ENV),
                    new Flaky("d", 2, SLOW));
            assertEquals(SLOW, ResultReporter.slowest(results));
        }

        @Test
        @DisplayName("list rong -> tra ZERO, khong nem NoSuchElementException")
        void slowestOnEmptyList() {
            assertEquals(Duration.ZERO, ResultReporter.slowest(List.of()));
        }
    }

    @Nested
    @DisplayName("Edge case ve so hoc - cho de sinh NaN/chia 0")
    class Arithmetic {

        @Test
        @DisplayName("flakinessRate tinh tren so test DA CHAY, khong tinh skipped")
        void flakinessExcludesSkipped() {
            List<TestResult> results = List.of(
                    new Flaky("a", 2, FAST),
                    new Passed("b", FAST),
                    new Skipped("c", NO_ENV),
                    new Skipped("d", NO_ENV));
            // executed = 2 (a, b), flaky = 1 -> 0.5. Neu chia cho 4 se ra 0.25 (sai).
            assertEquals(0.5, ResultReporter.flakinessRate(results));
        }

        @Test
        @DisplayName("BAY: tat ca skipped -> phai tra 0.0, khong duoc tra NaN")
        void noExecutedTestsReturnsZeroNotNaN() {
            List<TestResult> allSkipped = List.of(new Skipped("a", "x"), new Skipped("b", "x"));
            double rate = ResultReporter.flakinessRate(allSkipped);
            assertEquals(0.0, rate);
            assertTrue(!Double.isNaN(rate), "NaN se lam vo moi dashboard phia sau");
        }
    }

    @Test
    @DisplayName("Text block: summary render dung, khong con indent cua source")
    void textBlockSummary() {
        List<TestResult> results = List.of(
                new Passed("a", FAST), new Flaky("b", 2, SLOW), new Failed("c", "boom", List.of()));
        String summary = ResultReporter.summary(results);

        // Text block da strip indent -> dong bat dau tu cot 0.
        assertTrue(summary.startsWith("Test run summary"), summary);
        assertTrue(summary.contains("passed  : 1"), summary);
        assertTrue(summary.contains("flaky   : 1"), summary);
        assertTrue(summary.contains("failed  : 1"), summary);
        assertTrue(summary.contains("skipped : 0"), summary);
        assertTrue(summary.contains("slowest : 9000 ms"), summary);
    }
}
