package com.prep.cs.counting;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

/**
 * Mot tien trinh aggregator: consumer trong group, moi partition duoc giao co mot PartitionAggregator.
 * Offset KHONG commit vao Kafka (enable.auto.commit=false): nguon su that la bang agg_offset trong Postgres,
 * ghi cung transaction voi so dem. Khi nhan partition:
 *   1. doc next_offset tu Postgres,
 *   2. lui ve dau cua so khu trung (offsetsForTimes) va doc lai TOI next_offset chi de nap state khu trung,
 *   3. tu next_offset tro di moi dem.
 */
public final class Aggregator implements AutoCloseable {

    private final String name, topic;
    private final CountStore store;
    private final long commitIntervalMs, dedupWindowMs;
    private final KafkaConsumer<byte[], byte[]> consumer;
    private final Connection db;
    final Map<Integer, PartitionAggregator> parts = new ConcurrentHashMap<>();
    private volatile boolean running = true, crashed = false;
    private final Thread thread;
    public volatile long fencedCommits;

    public Aggregator(String name, String bootstrap, String topic, String group, CountStore store,
                      long commitIntervalMs, long dedupWindowMs) throws SQLException {
        this.name = name;
        this.topic = topic;
        this.store = store;
        this.commitIntervalMs = commitIntervalMs;
        this.dedupWindowMs = dedupWindowMs;
        this.db = store.connect();
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ConsumerConfig.GROUP_ID_CONFIG, group);
        p.put(ConsumerConfig.CLIENT_ID_CONFIG, name);
        p.put(ConsumerConfig.GROUP_PROTOCOL_CONFIG, "consumer");
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);         // offset nam o Postgres
        p.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 200);              // 200 block x ~2.000 request
        p.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 64 * 1024);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        this.consumer = new KafkaConsumer<>(p);
        this.thread = new Thread(this::loop, name);
    }

    public Aggregator start() {
        thread.start();
        return this;
    }

    private void loop() {
        consumer.subscribe(List.of(topic), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> tps) {
                for (TopicPartition tp : tps) assign(tp);
            }

            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> tps) {
                for (TopicPartition tp : tps) {
                    PartitionAggregator pa = parts.remove(tp.partition());
                    if (pa != null && !crashed) commitOne(pa);   // giao lai sach se: commit phan dang do
                }
            }

            @Override
            public void onPartitionsLost(Collection<TopicPartition> tps) {
                tps.forEach(tp -> parts.remove(tp.partition())); // co the da co chu moi: KHONG commit
            }
        });
        long lastCommit = System.currentTimeMillis();
        try {
            while (running) {
                for (ConsumerRecord<byte[], byte[]> rec : consumer.poll(Duration.ofMillis(100))) {
                    PartitionAggregator pa = parts.get(rec.partition());
                    if (pa != null) pa.onBlock(rec.offset(), rec.timestamp(), ByteBuffer.wrap(rec.value()));
                }
                if (System.currentTimeMillis() - lastCommit >= commitIntervalMs) {
                    commitAll();
                    lastCommit = System.currentTimeMillis();
                }
            }
            if (!crashed) commitAll();
        } finally {
            if (crashed) consumer.close(org.apache.kafka.clients.consumer.CloseOptions.timeout(Duration.ZERO));
            else consumer.close();
            try {
                db.close();
            } catch (SQLException ignored) {
                // dong ket noi khi tat: khong con gi de lam
            }
        }
    }

    private void assign(TopicPartition tp) {
        try {
            long committed = store.loadOffset(db, topic, tp.partition());
            PartitionAggregator pa = new PartitionAggregator(tp.partition(), committed);
            long from = committed;
            if (committed > 0) {
                OffsetAndTimestamp ot = consumer.offsetsForTimes(
                        Map.of(tp, System.currentTimeMillis() - dedupWindowMs)).get(tp);
                long begin = consumer.beginningOffsets(List.of(tp)).get(tp);
                from = Math.max(begin, ot == null ? committed : Math.min(ot.offset(), committed));
            }
            parts.put(tp.partition(), pa);
            consumer.seek(tp, from);
            System.out.printf("[%s] nhan p%d: offset trong Postgres = %d, doc lai tu %d de nap state khu trung%n",
                    name, tp.partition(), committed, from);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private void commitAll() {
        for (PartitionAggregator pa : List.copyOf(parts.values())) {
            // bi tu choi = aggregator khac (xac song) vua commit partition nay truoc minh: state trong RAM khong con
            // khop voi Postgres. Nap lai offset + dung lai state khu trung, KHONG buong partition: neu minh moi la chu
            // that thi buong ra la partition dung toi lan rebalance sau. Neu minh la xac song, lan poll() ke tiep se
            // bao onPartitionsLost va bo state.
            if (!commitOne(pa)) assign(new TopicPartition(topic, pa.partition));
        }
    }

    /** false = Postgres tu choi (offset trong DB khong con la offset minh bat dau): bo state cua partition nay. */
    private boolean commitOne(PartitionAggregator pa) {
        try {
            if (pa.commit(store, db, topic)) return true;
            fencedCommits++;
            parts.remove(pa.partition);
            System.out.printf("[%s] commit p%d bi tu choi: offset trong Postgres da doi, bo state%n", name, pa.partition);
            return false;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Tat em: commit het roi roi group. */
    @Override
    public void close() throws InterruptedException {
        running = false;
        thread.join();
    }

    /** Gia lap chet dot ngot SAU khi da xu ly nhung TRUOC khi commit: delta dang do mat het, offset khong doi. */
    public void crash() throws InterruptedException {
        crashed = true;
        running = false;
        thread.join();
    }

    public Collection<PartitionAggregator> partitions() {
        return parts.values();
    }

    public String name() {
        return name;
    }
}
