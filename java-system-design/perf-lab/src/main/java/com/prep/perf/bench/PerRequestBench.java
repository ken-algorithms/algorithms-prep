package com.prep.perf.bench;

import com.prep.perf.code.P01ExpensiveObjects;
import com.prep.perf.code.P01ExpensiveObjects.Transfer;
import com.prep.perf.code.P02Regex;
import com.prep.perf.code.P04ExceptionFlow;
import com.prep.perf.code.P06Logging;
import com.prep.perf.code.P06Logging.Payment;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Nhom 1 - chi phi MOI REQUEST. Moi cap {@code xxxBad / xxxGood} la cung mot viec.
 *
 * <p>Doc ket qua theo hai cot: {@code ns/op} (CPU) va {@code gc.alloc.rate.norm} (byte rac moi
 * request, can {@code -prof gc}). Nhan ca hai voi QPS de ra so core va MB/s rac ma he thong phai tra.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class PerRequestBench {

    private final Transfer transfer = new Transfer("tx-000123", "101-22334455", "202-99887766",
            125_000_00L, "VND", 1_790_000_000_000L);
    private final Instant at = Instant.ofEpochMilli(1_790_000_000_000L);
    private final Payment payment = new Payment("pay-42", "101223344556", 1_234_500L, "salary oct");
    private final String account = "101-22334455";
    private final String tags = "salary , bonus,  transfer ,vip , q4";

    // ---------- P01 ----------
    @Benchmark
    public String p01_json_newMapperPerCall() throws Exception {
        return P01ExpensiveObjects.toJsonBad(transfer);
    }

    @Benchmark
    public String p01_json_sharedMapper() throws Exception {
        return P01ExpensiveObjects.toJsonGood(transfer);
    }

    @Benchmark
    public String p01_date_newFormatterPerCall() {
        return P01ExpensiveObjects.formatBad(at);
    }

    @Benchmark
    public String p01_date_sharedFormatter() {
        return P01ExpensiveObjects.formatGood(at);
    }

    // ---------- P02 ----------
    @Benchmark
    public boolean p02_regex_stringMatches() {
        return P02Regex.isValidAccountBad(account);
    }

    @Benchmark
    public boolean p02_regex_precompiled() {
        return P02Regex.isValidAccountGood(account);
    }

    @Benchmark
    public String[] p02_split_regexEachCall() {
        return P02Regex.splitTagsBad(tags);
    }

    @Benchmark
    public String[] p02_split_precompiled() {
        return P02Regex.splitTagsGood(tags);
    }

    // ---------- P06 ----------
    @Benchmark
    public void p06_log_concatWhenDisabled() {
        P06Logging.logBad(payment);
    }

    @Benchmark
    public void p06_log_guarded() {
        P06Logging.logGuarded(payment);
    }

    @Benchmark
    public void p06_log_supplier() {
        P06Logging.logSupplier(payment);
    }

    /** P04 tach rieng vi co tham so do sau stack. */
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Warmup(iterations = 3, time = 1)
    @Measurement(iterations = 5, time = 1)
    @Fork(1)
    @State(Scope.Thread)
    public static class ExceptionBench {
        /** 0 = stack nong cua microbenchmark; 150 = gan voi stack Spring MVC + AOP + JPA. */
        @Param({"0", "150"})
        public int depth;

        /** 1 trong 2 input la sai dinh dang - "loi" la chuyen thuong. */
        private final String[] inputs = {"1500", "abc", "42", "12x", "-7", "", "99", "n/a"};
        private int i;

        @Benchmark
        public void p04_parse_exceptionAsFlow(Blackhole bh) {
            String s = inputs[i++ & 7];
            bh.consume(P04ExceptionFlow.atDepth(depth, () -> P04ExceptionFlow.parseOrDefaultBad(s, 0)));
        }

        @Benchmark
        public void p04_parse_precheck(Blackhole bh) {
            String s = inputs[i++ & 7];
            bh.consume(P04ExceptionFlow.atDepth(depth, () -> P04ExceptionFlow.parseOrDefaultGood(s, 0)));
        }
    }
}
