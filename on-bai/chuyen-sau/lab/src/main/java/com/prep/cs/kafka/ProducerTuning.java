package com.prep.cs.kafka;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.serialization.ByteArraySerializer;

/**
 * Cung mot luong record (event JSON ~94 byte), doi cau hinh producer va doc METRIC CUA CHINH CLIENT de
 * thay ben trong: batch to bao nhieu, moi request mang bao nhieu record, nen duoc bao nhieu, cho bao lau.
 * May lab dung chung CPU voi 6 container Kafka nen so dao dong: moi cau hinh chay 3 lan, in lan o GIUA (median).
 *
 *   scripts/run.sh com.prep.cs.kafka.ProducerTuning localhost:19092 perf 300000
 */
public final class ProducerTuning {

    record Cfg(String name, String acks, int lingerMs, int batchBytes, String compression) {}

    public static void main(String[] a) throws Exception {
        String bootstrap = a[0], topic = a[1];
        int n = Integer.parseInt(a[2]);
        List<Cfg> cfgs = List.of(
                new Cfg("A mac dinh < 4.0", "all", 0, 16_384, "none"),   // linger.ms mac dinh = 0 truoc Kafka 4.0
                new Cfg("B mac dinh 4.x", "all", 5, 16_384, "none"),     // KIP-1030: linger.ms mac dinh = 5
                new Cfg("C batch 64K", "all", 5, 65_536, "none"),
                new Cfg("D 256K + lz4", "all", 20, 262_144, "lz4"),
                new Cfg("E 256K + zstd", "all", 20, 262_144, "zstd"),
                new Cfg("F nhu D, acks=1", "1", 20, 262_144, "lz4"));
        byte[][] payloads = samplePayloads(1_000);
        System.out.printf("%d record, value trung binh %d byte, topic %s%n%n", n,
                java.util.Arrays.stream(payloads).mapToInt(p -> p.length).sum() / payloads.length, topic);
        System.out.printf("%-20s %9s %8s %10s %9s %8s %9s %9s%n", "cau hinh", "rec/s", "MB/s",
                "batch(B)", "rec/req", "nen", "queue ms", "req ms");
        run(bootstrap, topic, n / 3, cfgs.get(0), payloads); // khoi dong JIT, khong in
        for (Cfg c : cfgs) {
            List<double[]> runs = new java.util.ArrayList<>();
            for (int i = 0; i < 3; i++) runs.add(run(bootstrap, topic, n, c, payloads));
            runs.sort(java.util.Comparator.comparingDouble(r -> r[0]));
            double[] m = runs.get(1);
            System.out.printf("%-20s %9.0f %8.1f %10.0f %9.1f %8.2f %9.1f %9.1f   (3 lan: %.0f / %.0f / %.0f)%n",
                    c.name(), m[0], m[1], m[2], m[3], m[4], m[5], m[6], runs.get(0)[0], m[0], runs.get(2)[0]);
        }
    }

    /** Tra ve {rec/s, MB/s, batch-size-avg, records-per-request-avg, compression-rate-avg, queue ms, request ms}. */
    static double[] run(String bootstrap, String topic, int n, Cfg c, byte[][] payloads) throws Exception {
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ProducerConfig.ACKS_CONFIG, c.acks());
        p.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, c.acks().equals("all"));
        p.put(ProducerConfig.LINGER_MS_CONFIG, c.lingerMs());
        p.put(ProducerConfig.BATCH_SIZE_CONFIG, c.batchBytes());
        p.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, c.compression());
        p.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 64L * 1024 * 1024);
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        AtomicLong bytes = new AtomicLong();
        try (KafkaProducer<byte[], byte[]> producer = new KafkaProducer<>(p)) {
            producer.partitionsFor(topic); // lay metadata truoc khi bam gio
            long t0 = System.nanoTime();
            for (int i = 0; i < n; i++) {
                byte[] v = payloads[i % payloads.length];
                bytes.addAndGet(v.length);
                // key null -> sticky partitioner: dien day mot batch cho mot partition roi moi doi partition
                producer.send(new ProducerRecord<>(topic, null, v));
            }
            producer.flush();
            double sec = (System.nanoTime() - t0) / 1e9;
            Map<String, Double> m = metrics(producer.metrics());
            TimeUnit.MILLISECONDS.sleep(300);
            return new double[]{n / sec, bytes.get() / sec / 1e6, m.get("batch-size-avg"),
                    m.get("records-per-request-avg"), m.get("compression-rate-avg"),
                    m.get("record-queue-time-avg"), m.get("request-latency-avg")};
        }
    }

    /** Metric cap producer (group producer-metrics). Ten giong JMX: kafka.producer:type=producer-metrics */
    static Map<String, Double> metrics(Map<MetricName, ? extends Metric> all) {
        Map<String, Double> out = new LinkedHashMap<>();
        all.forEach((k, v) -> {
            if (k.group().equals("producer-metrics") && v.metricValue() instanceof Double d) {
                out.put(k.name(), d);
            }
        });
        return out;
    }

    /** Event giong bai dem: tenant, batch, ts, mot cap key/value. Lap lai nhieu nen nen duoc. */
    static byte[][] samplePayloads(int count) {
        java.util.Random r = new java.util.Random(42);
        String[] keys = {"checkout_v2", "search", "login", "cart_add", "payment_3ds", "profile_edit", "fake_abc"};
        byte[][] out = new byte[count][];
        for (int i = 0; i < count; i++) {
            String json = "{\"tenant\":\"t" + r.nextInt(50) + "\",\"batch\":\"b-" + Long.toHexString(r.nextLong())
                    + "\",\"ts\":" + (1_792_000_000_000L + r.nextInt(1_000_000)) + ",\"key\":\""
                    + keys[r.nextInt(keys.length)] + "\",\"value\":" + r.nextBoolean() + "}";
            out[i] = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        return out;
    }
}
