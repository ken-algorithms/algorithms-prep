package com.prep.cs.counting.bench;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

import com.prep.cs.counting.CountStore;
import com.prep.cs.counting.Decoded;
import com.prep.cs.counting.IngestNode;
import com.prep.cs.counting.PartitionAggregator;
import com.prep.cs.counting.RequestRecord;

/**
 * Ghi san request (mac dinh 20.000 client x 100 seq, 5% gui trung) thanh block 2.000 ban ghi vao topic moi,
 * roi MOT luong aggregator doc het: khu trung, cong don, commit vao Postgres moi giay (offset + delta cung
 * transaction). Do: request/giay ma mot luong xu ly duoc, va phan thoi gian nam o Postgres.
 *
 *   scripts/run.sh com.prep.cs.counting.bench.AggregatorBench localhost:19092 [so client] [seq moi client]
 */
public final class AggregatorBench {

    public static void main(String[] a) throws Exception {
        String bs = a[0], topic = "agg-bench-" + System.currentTimeMillis() % 100_000;
        int clients = a.length > 1 ? Integer.parseInt(a[1]) : 20_000, perClient = a.length > 2 ? Integer.parseInt(a[2]) : 100;
        produce(bs, topic, 12, clients, perClient);
        CountStore store = new CountStore(CountStore.dataSource("jdbc:postgresql://localhost:55432/lab", "lab", "lab"));
        store.createSchema();
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bs);
        p.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        p.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 50 * 1024 * 1024);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        Map<Integer, PartitionAggregator> parts = new HashMap<>();
        long requests = 0, commitNanos = 0;
        try (KafkaConsumer<byte[], byte[]> c = new KafkaConsumer<>(p); Connection db = store.connect()) {
            List<TopicPartition> tps = c.partitionsFor(topic).stream()
                    .map(pi -> new TopicPartition(topic, pi.partition())).toList();
            c.assign(tps);
            c.seekToBeginning(tps);
            Map<TopicPartition, Long> end = c.endOffsets(tps);
            for (TopicPartition tp : tps) {
                try (var st = db.prepareStatement("DELETE FROM agg_offset WHERE topic = ? AND part = ?")) {
                    st.setString(1, topic);
                    st.setInt(2, tp.partition());
                    st.executeUpdate();
                }
                parts.put(tp.partition(), new PartitionAggregator(tp.partition(), store.loadOffset(db, topic, tp.partition())));
            }
            long t0 = System.nanoTime(), lastCommit = System.currentTimeMillis();
            while (tps.stream().anyMatch(tp -> c.position(tp) < end.get(tp))) {
                for (ConsumerRecord<byte[], byte[]> rec : c.poll(Duration.ofMillis(200))) {
                    ByteBuffer b = ByteBuffer.wrap(rec.value());
                    requests += b.getInt(0);
                    parts.get(rec.partition()).onBlock(rec.offset(), rec.timestamp(), b);
                }
                if (System.currentTimeMillis() - lastCommit >= 1_000) {
                    long c0 = System.nanoTime();
                    for (PartitionAggregator pa : parts.values()) pa.commit(store, db, topic);
                    commitNanos += System.nanoTime() - c0;
                    lastCommit = System.currentTimeMillis();
                }
            }
            long c0 = System.nanoTime();
            for (PartitionAggregator pa : parts.values()) pa.commit(store, db, topic);
            commitNanos += System.nanoTime() - c0;
            double sec = (System.nanoTime() - t0) / 1e9;
            long dup = parts.values().stream().mapToLong(x -> x.duplicates).sum();
            long counted = parts.values().stream().mapToLong(x -> x.counted).sum();
            long commits = parts.values().stream().mapToLong(x -> x.commits).sum();      // commit co ghi that
            System.out.printf("1 luong, %d partition: %,d request trong %.1f s = %,.0f request/s; dem %,d, bo trung %,d; "
                            + "%d lan commit, tong %.0f ms trong Postgres (%.1f%% thoi gian)%n", tps.size(), requests, sec,
                    requests / sec, counted, dup, commits, commitNanos / 1e6, 100 * commitNanos / 1e9 / sec);
        }
    }

    /** clients x perClient request, moi client mot partition (nhu IngestNode), 5% request gui hai lan. */
    static void produce(String bs, String topic, int partitions, int clients, int perClient) throws Exception {
        try (var admin = org.apache.kafka.clients.admin.Admin.create(Map.of("bootstrap.servers", bs))) {
            admin.createTopics(List.of(new org.apache.kafka.clients.admin.NewTopic(topic, partitions, (short) 3))).all().get();
        }
        Properties p = new Properties();
        p.put("bootstrap.servers", bs);
        p.put("linger.ms", 20);
        p.put("batch.size", 512 * 1024);
        p.put("compression.type", "lz4");
        p.put("max.request.size", 4 * 1024 * 1024);
        p.put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        p.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        java.util.SplittableRandom r = new java.util.SplittableRandom(4);
        ByteBuffer[] blocks = new ByteBuffer[partitions];
        int[] n = new int[partitions];
        long total = 0;
        Decoded d = new Decoded();
        try (var producer = new org.apache.kafka.clients.producer.KafkaProducer<byte[], byte[]>(p)) {
            for (int seq = 0; seq < perClient; seq++) {
                for (int cl = 0; cl < clients; cl++) {
                    int copies = r.nextInt(100) < 5 ? 2 : 1;
                    for (int k = 0; k < copies; k++) {
                        d.clientId = 50_000 + cl;
                        d.epoch = 1;
                        d.seq = seq;
                        d.tsMillis = 1_792_390_000_000L + (seq * 1_000L) + r.nextInt(1_000);
                        d.counts[0] = 12; d.counts[1] = 4; d.counts[2] = 3; d.counts[3] = 1;
                        int part = IngestNode.partitionOf(d.clientId, partitions);
                        if (blocks[part] == null) {
                            blocks[part] = ByteBuffer.allocate(4 + 2_000 * RequestRecord.SIZE);
                            blocks[part].putInt(0);
                        }
                        RequestRecord.write(blocks[part], d);
                        total++;
                        if (++n[part] == 2_000) {
                            send(producer, topic, part, blocks[part], n[part]);
                            blocks[part] = null;
                            n[part] = 0;
                        }
                    }
                }
            }
            for (int part = 0; part < partitions; part++) if (n[part] > 0) send(producer, topic, part, blocks[part], n[part]);
        }
        System.out.printf("da ghi %,d request (%,d client x %d seq, ~5%% trung) vao %s%n", total, clients, perClient, topic);
    }

    static void send(org.apache.kafka.clients.producer.KafkaProducer<byte[], byte[]> producer, String topic, int part,
                     ByteBuffer block, int n) {
        block.putInt(0, n);
        byte[] v = java.util.Arrays.copyOf(block.array(), block.position());
        producer.send(new org.apache.kafka.clients.producer.ProducerRecord<>(topic, part, null, v));
    }
}
