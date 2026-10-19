package com.prep.cs.counting.bench;

import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;

import com.prep.cs.counting.Hash128;
import com.prep.cs.counting.IngestNode;
import com.prep.cs.counting.KeyRegistry;
import com.prep.cs.counting.LoadGen;

/**
 * Mot ingest node day het suc: T luong submit lien tuc (khong cho tung request, gioi han so dang cho xac nhan),
 * do so request/giay duoc Kafka XAC NHAN (acks=all), byte/request thuc su ghi xuong Kafka sau nen.
 * Body da sinh san (chi phi sinh JSON khong tinh vao).
 *
 *   scripts/run.sh com.prep.cs.counting.bench.IngestBench localhost:19092,localhost:29092,localhost:39092 3 15
 *   ... IngestBench <bootstrap> <luong> <giay> [so partition, mac dinh 12] [flush ms, mac dinh 5]
 */
public final class IngestBench {

    public static void main(String[] a) throws Exception {
        String bs = a[0];
        int threads = Integer.parseInt(a[1]), seconds = Integer.parseInt(a[2]);
        int partitions = a.length > 3 ? Integer.parseInt(a[3]) : 12, flushMs = a.length > 4 ? Integer.parseInt(a[4]) : 5;
        String topic = "ingest-bench-" + System.currentTimeMillis() % 100_000;
        try (Admin admin = Admin.create(Map.of("bootstrap.servers", bs))) {
            admin.createTopics(List.of(new NewTopic(topic, partitions, (short) 3))).all().get();
        }
        List<String> keys = IntStream.range(0, 1_000_000).mapToObj(k -> "feature_" + k).toList();
        KeyRegistry reg = KeyRegistry.of(keys, new Hash128(1, 2), 1);
        LoadGen gen = new LoadGen(keys);
        SplittableRandom r = new SplittableRandom(3);
        byte[][] bodies = new byte[8192][];
        long bodyBytes = 0;
        for (int i = 0; i < bodies.length; i++) {
            bodies[i] = gen.body(10_000 + i, 1, i, 1_792_390_000_000L + i * 13L, 20, r, new long[4]);
            bodyBytes += bodies[i].length;
        }
        System.out.printf("topic %s, %d partition, flush %d ms; body trung binh %d byte, 20 item; %d luong submit%n",
                topic, partitions, flushMs, bodyBytes / bodies.length, threads);
        AtomicLong ok = new AtomicLong(), failed = new AtomicLong();
        try (IngestNode node = new IngestNode(bs, topic, partitions, () -> reg, 2_000, flushMs)) {
            running = true;
            Thread[] ts = new Thread[threads];
            for (int t = 0; t < threads; t++) {
                int seed = t;
                ts[t] = new Thread(() -> {
                    Semaphore inflight = new Semaphore(20_000);    // backpressure: khong de RAM phinh vo han
                    int i = seed * 1_000;
                    while (running) {
                        inflight.acquireUninterruptibly();
                        node.submit(bodies[i++ & 8191]).whenComplete((v, e) -> {
                            if (e == null) ok.incrementAndGet();
                            else failed.incrementAndGet();
                            inflight.release();
                        });
                    }
                });
                ts[t].start();
            }
            Thread.sleep(3_000);                                   // khoi dong JIT, lap day pipeline
            long ok0 = ok.get(), acc0 = node.accepted.get(), bytes0 = node.bytesSent.get(), blocks0 = node.blocksSent.get();
            long alloc0 = ALLOC.getTotalThreadAllocatedBytes(), gc0 = gcCount(), gcMs0 = gcMillis();
            long t0 = System.nanoTime();
            for (int s = 1; s <= seconds; s++) {
                long before = ok.get();
                Thread.sleep(1_000);
                System.out.printf("  giay %2d: %,8d request duoc xac nhan%n", s, ok.get() - before);
            }
            double sec = (System.nanoTime() - t0) / 1e9;
            long n = ok.get() - ok0, bytes = node.bytesSent.get() - bytes0, blocks = node.blocksSent.get() - blocks0;
            long acc = node.accepted.get() - acc0;   // da dua vao block (phia gui): de chia byte va block cho dung
            long alloc = ALLOC.getTotalThreadAllocatedBytes() - alloc0, gcs = gcCount() - gc0, gcMs = gcMillis() - gcMs0;
            running = false;
            for (Thread t : ts) t.join();
            System.out.printf("TONG: %,.0f request/s (%,.0f item/s), loi %d; truoc nen %.1f byte/request vao Kafka "
                            + "(body JSON %d byte) -> %.0f lan nho hon%n", n / sec, n * 20 / sec, failed.get(),
                    (double) bytes / acc, bodyBytes / bodies.length, (bodyBytes / (double) bodies.length) / ((double) bytes / acc));
            System.out.printf("block: %,.0f record Kafka/s, trung binh %.1f request/block (toi da 2.000)%n",
                    blocks / sec, (double) acc / blocks);
            System.out.printf("cap phat: %,.0f byte/request (ca tien trinh); GC: %d lan, %d ms trong %.0f s%n",
                    (double) alloc / acc, gcs, gcMs, sec);
        }
        Thread.sleep(2_000);
        long onDisk = topicBytesOneReplica(bs, topic);
        System.out.printf("Tren dia (lz4, mot ban sao): %,d byte cho %,d request = %.1f byte/request%n",
                onDisk, ok.get(), (double) onDisk / ok.get());
    }

    /** Tong kich thuoc cac partition cua topic tren moi broker, chia cho replication factor 3. */
    static long topicBytesOneReplica(String bs, String topic) throws Exception {
        try (Admin admin = Admin.create(Map.of("bootstrap.servers", bs))) {
            List<Integer> brokers = admin.describeCluster().nodes().get().stream().map(n -> n.id()).toList();
            long total = 0;
            for (var perBroker : admin.describeLogDirs(brokers).allDescriptions().get().values()) {
                for (var dir : perBroker.values()) {
                    for (var e : dir.replicaInfos().entrySet()) {
                        if (e.getKey().topic().equals(topic)) total += e.getValue().size();
                    }
                }
            }
            return total / 3;
        }
    }

    private static volatile boolean running;

    /** Tong byte cap phat cua moi luong dang song (HotSpot): chia cho so request ra rac moi request. */
    private static final com.sun.management.ThreadMXBean ALLOC =
            (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();

    private static long gcCount() {
        return java.lang.management.ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(java.lang.management.GarbageCollectorMXBean::getCollectionCount).sum();
    }

    private static long gcMillis() {
        return java.lang.management.ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(java.lang.management.GarbageCollectorMXBean::getCollectionTime).sum();
    }
}
