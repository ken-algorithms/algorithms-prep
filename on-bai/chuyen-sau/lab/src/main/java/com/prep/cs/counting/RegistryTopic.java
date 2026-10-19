package com.prep.cs.counting;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * Registry key hop le phat cho moi ingest node qua MOT topic compacted (bai 02): key Kafka = ten key,
 * value "1" = hop le, value null (tombstone) = bo key. Compaction giu ban moi nhat moi key, nen node moi khoi
 * dong doc tu dau toi cuoi topic la co dung tap hien tai, khong phai doc ca lich su.
 * Sau do node theo doi tiep (follow) va thay KeyRegistry moi bang mot phep gan tham chieu.
 */
public final class RegistryTopic implements AutoCloseable {

    private final String bootstrap, topic;
    private final Hash128 hash;
    private final AtomicReference<KeyRegistry> current = new AtomicReference<>();
    private final Set<String> keys = new HashSet<>();
    private KafkaConsumer<String, String> consumer;
    private Thread follower;
    private volatile boolean running;

    public RegistryTopic(String bootstrap, String topic, Hash128 hash) {
        this.bootstrap = bootstrap;
        this.topic = topic;
        this.hash = hash;
    }

    public static void create(String bootstrap, String topic) throws Exception {
        try (Admin admin = Admin.create(Map.of("bootstrap.servers", bootstrap))) {
            admin.createTopics(List.of(new NewTopic(topic, 1, (short) 3).configs(Map.of(
                    TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT)))).all().get();
        }
    }

    /** valid=true ghi "1", valid=false ghi tombstone (xoa key). */
    public static void publish(String bootstrap, String topic, Map<String, Boolean> changes) {
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ProducerConfig.LINGER_MS_CONFIG, 20);
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(p)) {
            changes.forEach((k, ok) -> producer.send(new ProducerRecord<>(topic, k, ok ? "1" : null)));
        }
    }

    /** Doc tu dau toi end offset hien tai, dung KeyRegistry, roi chay luong theo doi cap nhat. */
    public KeyRegistry loadAndFollow() {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumer = new KafkaConsumer<>(p);                  // khong group: moi node doc TOAN BO topic
        TopicPartition tp = new TopicPartition(topic, 0);
        consumer.assign(List.of(tp));
        consumer.seekToBeginning(List.of(tp));
        long end = consumer.endOffsets(List.of(tp)).get(tp);
        while (consumer.position(tp) < end) apply(consumer.poll(Duration.ofMillis(200)));
        current.set(KeyRegistry.of(keys, hash, end));
        running = true;
        follower = new Thread(() -> {
            while (running) {
                var batch = consumer.poll(Duration.ofMillis(200));
                if (!batch.isEmpty()) {
                    apply(batch);
                    current.set(KeyRegistry.of(keys, hash, consumer.position(tp))); // copy-on-write, doi tham chieu
                }
            }
            consumer.close();
        }, "registry-follower");
        follower.start();
        return current.get();
    }

    private void apply(Iterable<ConsumerRecord<String, String>> batch) {
        for (ConsumerRecord<String, String> r : batch) {
            if (r.value() == null) keys.remove(r.key());
            else keys.add(r.key());
        }
    }

    public KeyRegistry current() {
        return current.get();
    }

    @Override
    public void close() throws InterruptedException {
        running = false;
        if (follower != null) follower.join();
    }
}
