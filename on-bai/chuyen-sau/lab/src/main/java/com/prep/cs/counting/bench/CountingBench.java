package com.prep.cs.counting.bench;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prep.cs.counting.ClientWindow;
import com.prep.cs.counting.Decoded;
import com.prep.cs.counting.Hash128;
import com.prep.cs.counting.KeyRegistry;
import com.prep.cs.counting.LoadGen;
import com.prep.cs.counting.Outcome;
import com.prep.cs.counting.PartitionAggregator;
import com.prep.cs.counting.RequestDecoder;
import com.prep.cs.counting.RequestRecord;

/**
 * Chi phi CPU tren MOT core cua tung buoc duong nong. Body that ~1 KB, 20 item; registry 1 trieu key.
 *
 *   mvn -q compile && java -cp "target/classes:$(cat target/cp.txt)" org.openjdk.jmh.Main CountingBench -prof gc
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1, jvmArgs = {"-Xms2g", "-Xmx2g"})
public class CountingBench {

    static final int KEYS = 1_000_000;
    List<String> keys;
    KeyRegistry registry;
    Set<String> hashSet;
    byte[][] bodies;
    int i;
    final RequestDecoder decoder = new RequestDecoder();
    final Decoded out = new Decoded();
    final ObjectMapper mapper = new ObjectMapper();
    String[] probeKeys;
    long[][] probeHashes;
    ClientWindow window;
    long seq;
    ByteBuffer block;
    PartitionAggregator agg;
    long offset;

    @Setup(Level.Trial)
    public void setup() {
        keys = IntStream.range(0, KEYS).mapToObj(k -> "feature_" + k).toList();
        Hash128 h = new Hash128(0x9E3779B97F4A7C15L, 0xC2B2AE3D27D4EB4FL);
        registry = KeyRegistry.of(keys, h, 1);
        hashSet = new HashSet<>(keys);
        LoadGen gen = new LoadGen(keys);
        SplittableRandom r = new SplittableRandom(1);
        bodies = new byte[1024][];
        for (int b = 0; b < bodies.length; b++) {
            bodies[b] = gen.body(1_000 + b, 1, b, 1_792_390_000_000L + b, 20, r, new long[4]);
        }
        probeKeys = new String[4096];
        probeHashes = new long[4096][2];
        for (int p = 0; p < probeKeys.length; p++) {
            probeKeys[p] = p % 4 == 0 ? "fake_" + Long.toHexString(r.nextLong()) : keys.get(r.nextInt(KEYS));
            h.hash(probeKeys[p], probeHashes[p]);
        }
        window = new ClientWindow(1, 0, 0);
        // mot block 2.000 request cua 200 client: dung de do phia aggregator
        block = ByteBuffer.allocate(4 + 2_000 * RequestRecord.SIZE);
        block.putInt(2_000);
        Decoded d = new Decoded();
        for (int k = 0; k < 2_000; k++) {
            d.clientId = 1_000 + k % 200;
            d.epoch = 1;
            d.seq = k / 200;
            d.tsMillis = 1_792_390_000_000L + k * 7L;
            d.counts[0] = 12;
            d.counts[1] = 4;
            d.counts[2] = 3;
            d.counts[3] = 1;
            RequestRecord.write(block, d);
        }
        agg = new PartitionAggregator(0, 0);
    }

    /** Cach cua bai nay: parser streaming + registry fingerprint, khong POJO, khong String cho key. */
    @Benchmark
    public int decodeStreaming() {
        byte[] b = bodies[i++ & 1023];
        decoder.decode(b, 0, b.length, registry, out);
        return out.counts[0];
    }

    /** Cach "binh thuong": doc ca cay JSON, key thanh String, tra HashSet<String>. */
    @Benchmark
    public int decodeTreeAndHashSet() throws Exception {
        byte[] b = bodies[i++ & 1023];
        JsonNode root = mapper.readTree(b);
        int[] c = new int[4];
        for (JsonNode item : root.get("items")) {
            JsonNode k = item.get("key"), v = item.get("value");
            Outcome o;
            if (k == null || !k.isTextual()) o = Outcome.MALFORMED;
            else if (!hashSet.contains(k.asText())) o = Outcome.FAKE_KEY;
            else if (v != null && v.isBoolean()) o = v.booleanValue() ? Outcome.TRUE : Outcome.FALSE;
            else o = Outcome.MALFORMED;
            c[o.ordinal()]++;
        }
        return c[0];
    }

    @Benchmark
    public boolean lookupRegistryPreHashed() {
        long[] h = probeHashes[i++ & 4095];
        return registry.contains(h[0], h[1]);
    }

    @Benchmark
    public boolean lookupHashSetString() {
        return hashSet.contains(probeKeys[i++ & 4095]);
    }

    @Benchmark
    public ClientWindow.Verdict clientWindowAccept() {
        return window.accept(1, seq++, 0);
    }

    /**
     * Ca block 2.000 request (200 client x 10 request): khu trung + cong vao 4 tang. Moi vong doi seq sang 10 so
     * moi de moi request deu la request MOI (duong dem, khong phai duong bo trung). Chia cho 2.000 ra ns/request.
     */
    @Benchmark
    public long aggregateBlockOf2000() {
        long base = (offset + 1) * 10;
        for (int k = 0; k < 2_000; k++) block.putLong(4 + k * RequestRecord.SIZE + 12, base + k / 200);
        block.rewind();
        agg.onBlock(offset++, 0, block);
        return agg.counted;
    }

    /**
     * Phien ban dau tien cua aggregator (giu lai de so sanh): HashMap<Long, ClientWindow> (boxing moi lan tra)
     * va TreeMap<Cell, long[]> cong NGAY vao 4 tang cho moi request (4 Cell moi + 4 lan so sanh cay).
     */
    static final class NaiveAggregation {
        final java.util.Map<Long, ClientWindow> clients = new java.util.HashMap<>();
        final java.util.TreeMap<com.prep.cs.counting.CountStore.Cell, long[]> pending = new java.util.TreeMap<>();
        final Decoded r = new Decoded();
        long counted;

        void onBlock(ByteBuffer block) {
            int n = block.getInt();
            for (int i = 0; i < n; i++) {
                RequestRecord.read(block, r);
                ClientWindow w = clients.get(r.clientId);
                ClientWindow.Verdict v;
                if (w == null) {
                    clients.put(r.clientId, new ClientWindow(r.epoch, r.seq, 0));
                    v = ClientWindow.Verdict.NEW;
                } else {
                    v = w.accept(r.epoch, r.seq, 0);
                }
                if (v != ClientWindow.Verdict.NEW) continue;
                counted++;
                long sec = r.tsMillis / 1000;
                for (com.prep.cs.counting.Tier t : com.prep.cs.counting.Tier.values()) {
                    long[] c = pending.computeIfAbsent(new com.prep.cs.counting.CountStore.Cell(t, t.bucketOf(sec)),
                            k -> new long[4]);
                    for (int k = 0; k < 4; k++) c[k] += r.counts[k];
                }
            }
        }
    }

    NaiveAggregation naive = new NaiveAggregation();

    @Benchmark
    public long aggregateBlockOf2000Naive() {
        long base = (offset + 1) * 10;
        for (int k = 0; k < 2_000; k++) block.putLong(4 + k * RequestRecord.SIZE + 12, base + k / 200);
        block.rewind();
        offset++;
        naive.onBlock(block);
        return naive.counted;
    }

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
