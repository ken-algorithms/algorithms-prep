package com.prep.perf.bench;

import com.prep.perf.code.P11Contention;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * P11 - throughput (ops/us) cua bo dem dung chung khi nhieu thread cung danh vao.
 * Chay voi 1 thread va 4 thread de thay: ban synchronized KHONG tang (thuong giam) khi them thread.
 *
 * <pre>
 * java -jar target/benchmarks.jar ContentionBench -t 1
 * java -jar target/benchmarks.jar ContentionBench -t 4
 * </pre>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@Threads(4)
@State(Scope.Benchmark)
public class ContentionBench {

    /** 16 endpoint - nhu bo dem request theo route. */
    private static final String[] KEYS = new String[16];

    static {
        for (int i = 0; i < KEYS.length; i++) {
            KEYS[i] = "/api/v1/route-" + i;
        }
    }

    private final P11Contention.Counter sync = new P11Contention.SyncCounter();
    private final P11Contention.Counter adder = new P11Contention.AdderCounter();

    @Benchmark
    public void p11_counter_synchronizedMap() {
        sync.inc(KEYS[ThreadLocalRandom.current().nextInt(16)]);
    }

    @Benchmark
    public void p11_counter_chmLongAdder() {
        adder.inc(KEYS[ThreadLocalRandom.current().nextInt(16)]);
    }
}
