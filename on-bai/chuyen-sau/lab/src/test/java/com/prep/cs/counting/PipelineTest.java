package com.prep.cs.counting;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

import com.prep.cs.Infra;

/**
 * Toan tuyen tren Kafka + Postgres that: 200 client, 2 ingest node, aggregator chet giua chung, ingest node chet
 * giua chung, client retry, client khoi dong lai (epoch moi). Cuoi cung so trong Postgres phai KHOP CHINH XAC
 * voi oracle (dem luc sinh request), cho ca khoang lon lan khoang nho bat ky.
 */
class PipelineTest {

    static final Hash128 HASH = new Hash128(0x5DEECE66DL, 0x2545F4914F6CDD1DL);
    static final int PARTITIONS = 6, CLIENTS = 200, PER_CLIENT = 500, ITEMS = 20;
    static final long BASE_MS = Instant.parse("2026-10-19T09:58:30Z").toEpochMilli();
    static final long SPREAD_MS = 10 * 60_000;

    final Map<Long, long[]> oracle = new ConcurrentHashMap<>();       // giay -> 4 con dem
    final AtomicLong acked = new AtomicLong(), retries = new AtomicLong(), dupSent = new AtomicLong();

    @Test
    void exactCountsThroughCrashesRetriesAndRestarts() throws Exception {
        Infra.requireKafka();
        DataSource ds = Infra.requirePostgres();
        String suffix = Long.toString(System.currentTimeMillis() % 1_000_000);
        String topic = "count-req-" + suffix, regTopic = "key-registry-" + suffix, group = "agg-" + suffix;
        try (Admin admin = Admin.create(Map.of("bootstrap.servers", Infra.KAFKA))) {
            admin.createTopics(List.of(new NewTopic(topic, PARTITIONS, (short) 3))).all().get();
        }
        List<String> keys = IntStream.range(0, 10_000).mapToObj(i -> "feature_" + i).toList();
        RegistryTopic.create(Infra.KAFKA, regTopic);
        Map<String, Boolean> pub = new HashMap<>();
        keys.forEach(k -> pub.put(k, true));
        pub.put("legacy_removed", true);
        RegistryTopic.publish(Infra.KAFKA, regTopic, pub);
        RegistryTopic.publish(Infra.KAFKA, regTopic, Map.of("legacy_removed", false));   // tombstone
        CountStore store = new CountStore(ds);
        store.createSchema();
        store.truncate();

        try (RegistryTopic registry = new RegistryTopic(Infra.KAFKA, regTopic, HASH)) {
            KeyRegistry kr = registry.loadAndFollow();
            System.out.printf("[LAB] registry tu topic compacted: %d key (legacy_removed: %s)%n",
                    kr.size(), kr.contains("legacy_removed"));
            assertThat(kr.size()).isEqualTo(10_000);

            Aggregator a1 = new Aggregator("agg-1", Infra.KAFKA, topic, group, store, 500, 15 * 60_000).start();
            Aggregator a2 = new Aggregator("agg-2", Infra.KAFKA, topic, group, store, 500, 15 * 60_000).start();
            IngestNode n1 = new IngestNode(Infra.KAFKA, topic, PARTITIONS, registry::current, 2_000, 5);
            IngestNode n2 = new IngestNode(Infra.KAFKA, topic, PARTITIONS, registry::current, 2_000, 5);
            IngestNode[] nodes = {n1, n2};
            LoadGen gen = new LoadGen(keys);
            int total = CLIENTS * PER_CLIENT;
            long t0 = System.nanoTime();
            List<Aggregator> aggs = new ArrayList<>(List.of(a1, a2));
            Thread chaos = new Thread(() -> {
                try {
                    while (acked.get() < total * 3 / 10) Thread.sleep(20);
                    a1.crash();                     // chet sau khi xu ly, truoc khi commit
                    System.out.printf("[LAB] %.1fs %d/%d request da xac nhan: agg-1 CHET (delta chua commit mat het)%n",
                            (System.nanoTime() - t0) / 1e9, acked.get(), total);
                    while (acked.get() < total * 5 / 10) Thread.sleep(20);
                    aggs.add(newAggregator("agg-3", topic, group, store));
                    System.out.printf("[LAB] %.1fs %d/%d: agg-3 vao group (rebalance)%n",
                            (System.nanoTime() - t0) / 1e9, acked.get(), total);
                    while (acked.get() < total * 6 / 10) Thread.sleep(20);
                    n1.crash();                     // block trong RAM chua gui: client het gio, retry sang n2
                    System.out.printf("[LAB] %.1fs %d/%d: ingest node 1 CHET%n",
                            (System.nanoTime() - t0) / 1e9, acked.get(), total);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            chaos.start();
            try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int c = 0; c < CLIENTS; c++) {
                    long clientId = 1_000 + c;
                    pool.submit(() -> runClient(clientId, nodes, gen));
                }
            }
            chaos.join();
            double sec = (System.nanoTime() - t0) / 1e9;
            n2.close();
            waitCaughtUp(store, topic);
            for (Aggregator a : aggs) if (a != a1) a.close();

            long counted = 0, dups = 0, stale = 0, tooOld = 0;
            try (Connection c = store.connect(); Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT count(*) FROM count_cell")) {
                rs.next();
                System.out.printf("[LAB] %d request duy nhat trong %.1f s (%.0f request/s, %.0f item/s), "
                                + "retry %d, gui trung co chu y %d; Postgres co %d o%n", total + 10, sec,
                        (total + 10) / sec, (total + 10) * ITEMS / sec, retries.get(), dupSent.get(), rs.getLong(1));
            }
            long[] want = new long[4];
            oracle.values().forEach(v -> { for (int k = 0; k < 4; k++) want[k] += v[k]; });
            long from = BASE_MS / 1000, to = (BASE_MS + SPREAD_MS) / 1000 + 1;
            Stats all = store.query(from, to);
            System.out.printf("[LAB] oracle  TRUE=%d FALSE=%d FAKE_KEY=%d MALFORMED=%d%n", want[0], want[1], want[2], want[3]);
            System.out.printf("[LAB] Postgres %s%n", all);
            assertThat(all.sameCounts(want)).isTrue();

            SplittableRandom r = new SplittableRandom(5);
            for (int i = 0; i < 100; i++) {
                long a = from + r.nextLong(to - from), b = a + r.nextLong(to - a + 1);
                long[] w = new long[4];
                oracle.forEach((s, v) -> { if (s >= a && s < b) for (int k = 0; k < 4; k++) w[k] += v[k]; });
                assertThat(store.query(a, b).sameCounts(w)).as("[%d, %d)", a, b).isTrue();
            }
            System.out.println("[LAB] 100 khoang ngau nhien (do phan giai giay) deu khop oracle");
        }
    }

    static Aggregator newAggregator(String name, String topic, String group, CountStore store) {
        try {
            return new Aggregator(name, Infra.KAFKA, topic, group, store, 500, 15 * 60_000).start();
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Mot client: gui tuan tu, cho xac nhan; het gio thi retry DUNG body do sang node khac. */
    void runClient(long clientId, IngestNode[] nodes, LoadGen gen) {
        SplittableRandom r = new SplittableRandom(clientId);
        int epoch = 1;
        for (int i = 0; i < PER_CLIENT + (clientId < 1_010 ? 1 : 0); i++) {
            long seq = i;
            if (clientId < 1_010 && i == PER_CLIENT / 2) epoch = 2;              // 10 client khoi dong lai
            if (epoch == 2) seq = i - PER_CLIENT / 2;
            long ts = BASE_MS + r.nextLong(SPREAD_MS);
            long[] exp = new long[4];
            byte[] body = gen.body(clientId, epoch, seq, ts, ITEMS, r, exp);
            send(body, nodes, r.nextInt(2));
            oracle.merge(ts / 1000, exp, (x, y) -> { for (int k = 0; k < 4; k++) x[k] += y[k]; return x; });
            acked.incrementAndGet();
            if (r.nextInt(100) < 5) {                                             // mat response: gui lai
                dupSent.incrementAndGet();
                send(body, nodes, r.nextInt(2));
            }
        }
        if (clientId < 1_010) {                                                   // tien trinh cu "song lai"
            dupSent.incrementAndGet();
            send(gen.body(clientId, 1, 3, BASE_MS, ITEMS, r, new long[4]), nodes, 0);
        }
    }

    void send(byte[] body, IngestNode[] nodes, int first) {
        for (int attempt = 0; ; attempt++) {
            IngestNode n = nodes[(first + attempt) % nodes.length];
            try {
                n.submit(body).get(2, TimeUnit.SECONDS);
                return;
            } catch (Exception e) {
                retries.incrementAndGet();             // node chet / het gio: thu node khac, CUNG body
            }
        }
    }

    static void waitCaughtUp(CountStore store, String topic) throws Exception {
        try (Admin admin = Admin.create(Map.of("bootstrap.servers", Infra.KAFKA))) {
            Map<TopicPartition, OffsetSpec> q = new HashMap<>();
            for (int p = 0; p < PARTITIONS; p++) q.put(new TopicPartition(topic, p), OffsetSpec.latest());
            Map<Integer, Long> end = new HashMap<>();
            admin.listOffsets(q).all().get().forEach((tp, info) -> end.put(tp.partition(), info.offset()));
            long deadline = System.currentTimeMillis() + 60_000;
            while (System.currentTimeMillis() < deadline) {
                Map<Integer, Long> done = new HashMap<>();
                try (Connection c = store.connect(); Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("SELECT part, next_offset FROM agg_offset WHERE topic = '" + topic + "'")) {
                    while (rs.next()) done.put(rs.getInt(1), rs.getLong(2));
                }
                if (done.equals(end)) {
                    System.out.println("[LAB] aggregator da commit toi cuoi moi partition: " + end);
                    return;
                }
                Thread.sleep(200);
            }
            throw new AssertionError("aggregator khong theo kip trong 60 giay");
        }
    }
}
