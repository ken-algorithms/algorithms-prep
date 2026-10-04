package com.prep.perf;

import com.prep.perf.code.P01ExpensiveObjects;
import com.prep.perf.code.P02Regex;
import com.prep.perf.code.P03StringConcat;
import com.prep.perf.code.P04ExceptionFlow;
import com.prep.perf.code.P05Boxing;
import com.prep.perf.code.P07HiddenQuadratic;
import com.prep.perf.code.P11Contention;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Ban sua chi co gia tri khi no cho CUNG ket qua voi ban xau. Toc do do bang JMH, khong o day.
 */
class SameResultTest {

    @Test
    void p01_sharedMapperAndFormatterProduceSameOutput() throws Exception {
        var t = new P01ExpensiveObjects.Transfer("tx-1", "101-1", "202-2", 500L, "VND", 1L);
        assertEquals(P01ExpensiveObjects.toJsonBad(t), P01ExpensiveObjects.toJsonGood(t));
        var at = Instant.parse("2026-10-04T08:15:30Z");
        assertEquals("2026-10-04 08:15:30", P01ExpensiveObjects.formatGood(at));
        assertEquals(P01ExpensiveObjects.formatBad(at), P01ExpensiveObjects.formatGood(at));
    }

    @ParameterizedTest
    @ValueSource(strings = {"101-22334455", "101-123", "10-22334455", "abc-12345678", "101-12345678901", ""})
    void p02_precompiledRegexMatchesSame(String s) {
        assertEquals(P02Regex.isValidAccountBad(s), P02Regex.isValidAccountGood(s));
    }

    @Test
    void p02_splitSame() {
        String tags = "salary , bonus,  transfer ,vip , q4";
        assertArrayEquals(P02Regex.splitTagsBad(tags), P02Regex.splitTagsGood(tags));
    }

    @Test
    void p03_concatSame() {
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            rows.add("row-" + i);
        }
        assertEquals(P03StringConcat.csvBad(rows), P03StringConcat.csvGood(rows));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1500", "abc", "42", "12x", "-7", "+9", "-", "+", "", "n/a",
            "2147483647", "2147483648", "-2147483648", "-2147483649", "00012", "99999999999"})
    void p04_precheckParsesExactlyLikeParseInt(String s) {
        assertEquals(P04ExceptionFlow.parseOrDefaultBad(s, -1), P04ExceptionFlow.parseOrDefaultGood(s, -1));
    }

    @Test
    void p04_nullIsDefaultInBoth() {
        assertEquals(P04ExceptionFlow.parseOrDefaultBad(null, 3), P04ExceptionFlow.parseOrDefaultGood(null, 3));
    }

    @Test
    void p05_histogramAndSumSame() {
        var rnd = new SplittableRandom(1);
        int[] codes = rnd.ints(10_000, 0, 1000).toArray();
        Map<Integer, Integer> boxed = P05Boxing.histogramBad(codes);
        int[] prim = P05Boxing.histogramGood(codes, 1000);
        for (int k = 0; k < 1000; k++) {
            assertEquals(boxed.getOrDefault(k, 0), prim[k]);
        }
        long[] amounts = rnd.longs(10_000, 0, 1_000_000_000L).toArray();
        assertEquals(P05Boxing.sumBad(amounts), P05Boxing.sumGood(amounts));
    }

    @Test
    void p07_dedupeKeepsFirstOccurrenceOrder() {
        List<String> in = List.of("b", "a", "b", "c", "a", "d");
        assertEquals(List.of("b", "a", "c", "d"), P07HiddenQuadratic.dedupeGood(in));
        assertEquals(P07HiddenQuadratic.dedupeBad(in), P07HiddenQuadratic.dedupeGood(in));
    }

    @Test
    void p11_bothCountersAreExactUnderConcurrency() throws Exception {
        for (P11Contention.Counter c : List.of(new P11Contention.SyncCounter(), new P11Contention.AdderCounter())) {
            try (ExecutorService ex = Executors.newFixedThreadPool(8)) {
                for (int t = 0; t < 8; t++) {
                    ex.submit(() -> {
                        for (int i = 0; i < 10_000; i++) {
                            c.inc("k" + (i % 4));
                        }
                    });
                }
            }
            long total = 0;
            for (int k = 0; k < 4; k++) {
                total += c.get("k" + k);
            }
            assertEquals(80_000, total, c.getClass().getSimpleName());
        }
    }
}
