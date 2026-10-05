package com.prep.perf.bench;

import com.prep.perf.code.P03StringConcat;
import com.prep.perf.code.P05Boxing;
import com.prep.perf.code.P07HiddenQuadratic;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;

/**
 * Nhom 1 (tiep) - code chay tren MOT LO du lieu trong mot request: export, import, doi soat.
 * Tham so {@code n} cho thay do phuc tap: bad tang theo n^2, good tang theo n.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class BatchBench {

    @Param({"1000", "10000"})
    public int n;

    private List<String> rows;
    private List<String> idsWithDupes;
    private int[] codes;
    private long[] amounts;

    @Setup(Level.Trial)
    public void setup() {
        var rnd = new SplittableRandom(7);
        rows = new ArrayList<>(n);
        idsWithDupes = new ArrayList<>(n);
        codes = new int[n];
        amounts = new long[n];
        for (int i = 0; i < n; i++) {
            rows.add("tx-" + i + ",101-22334455,202-99887766," + rnd.nextInt(1_000_000) + ",VND");
            idsWithDupes.add("acct-" + rnd.nextInt(n / 2 + 1));
            codes[i] = rnd.nextInt(1000);
            amounts[i] = rnd.nextLong(1_000_000_000L);
        }
    }

    // ---------- P03 ----------
    @Benchmark
    public String p03_csv_plusInLoop() {
        return P03StringConcat.csvBad(rows);
    }

    @Benchmark
    public String p03_csv_stringBuilder() {
        return P03StringConcat.csvGood(rows);
    }

    // ---------- P05 ----------
    @Benchmark
    public Map<Integer, Integer> p05_histogram_boxedMap() {
        return P05Boxing.histogramBad(codes);
    }

    @Benchmark
    public int[] p05_histogram_primitiveArray() {
        return P05Boxing.histogramGood(codes, 1000);
    }

    @Benchmark
    public long p05_sum_boxedLong() {
        return P05Boxing.sumBad(amounts);
    }

    @Benchmark
    public long p05_sum_primitive() {
        return P05Boxing.sumGood(amounts);
    }

    // ---------- P07 ----------
    @Benchmark
    public List<String> p07_dedupe_listContains() {
        return P07HiddenQuadratic.dedupeBad(idsWithDupes);
    }

    @Benchmark
    public List<String> p07_dedupe_linkedHashSet() {
        return P07HiddenQuadratic.dedupeGood(idsWithDupes);
    }
}
