package com.prep.cs.kafka;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * Song song hoa phia doc = so partition. Topic 6 partition, moi record ton 1 ms xu ly (gia lap goi DB/HTTP).
 * Moi lan chay: dung N consumer, CHO group chia xong (khong tinh thoi gian rebalance), roi moi ghi n record
 * va bam gio tu record dau tien toi record cuoi cung. Ky vong: nhanh dan toi 6 consumer roi dung lai.
 *
 *   scripts/run.sh com.prep.cs.kafka.ScaleConsumers localhost:19092 6000
 */
public final class ScaleConsumers {

    public static void main(String[] a) throws Exception {
        String bs = a[0];
        int n = Integer.parseInt(a[1]);
        System.out.printf("6 partition, %d record moi lan, xu ly 1 ms/record%n", n);
        for (int consumers : new int[]{1, 2, 3, 6, 8}) {
            run(bs, n, consumers);
        }
    }

    static void run(String bs, int n, int consumers) throws Exception {
        String topic = "work-" + consumers + "-" + System.currentTimeMillis() % 100_000;
        try (Admin admin = Admin.create(Map.of("bootstrap.servers", bs))) {
            admin.createTopics(List.of(new NewTopic(topic, 6, (short) 3))).all().get();
        }
        AtomicLong done = new AtomicLong(), firstAt = new AtomicLong(), lastAt = new AtomicLong();
        AtomicBoolean stop = new AtomicBoolean();
        Map<String, AtomicLong> processed = new ConcurrentHashMap<>();
        Map<String, Set<TopicPartition>> assigned = new ConcurrentHashMap<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < consumers; i++) {
            String name = "c" + (i + 1);
            processed.put(name, new AtomicLong());
            Thread t = new Thread(() -> consume(bs, topic, name, n, done, firstAt, lastAt, stop, processed, assigned), name);
            threads.add(t);
            t.start();
        }
        // cho group on dinh: du 6 partition co chu va khong doi trong 3 giay
        String last = "";
        long stableSince = System.currentTimeMillis();
        while (System.currentTimeMillis() - stableSince < 3_000) {
            String now = new TreeMap<>(assigned).toString();
            int total = assigned.values().stream().mapToInt(Set::size).sum();
            if (!now.equals(last) || total != 6) {
                last = now;
                stableSince = System.currentTimeMillis();
            }
            Thread.sleep(200);
        }
        produce(bs, topic, n);
        for (Thread t : threads) t.join(120_000);
        stop.set(true);
        double sec = (lastAt.get() - firstAt.get()) / 1e9;
        Map<String, String> view = new TreeMap<>();
        processed.forEach((k, v) -> view.put(k, assigned.getOrDefault(k, Set.of()).stream()
                .map(TopicPartition::partition).sorted().toList() + "=" + v.get()));
        System.out.printf("%d consumer: %5.2f s, %5.0f record/s   %s%n", consumers, sec, n / sec, view);
    }

    static void consume(String bs, String topic, String name, int n, AtomicLong done, AtomicLong firstAt,
                        AtomicLong lastAt, AtomicBoolean stop, Map<String, AtomicLong> processed,
                        Map<String, Set<TopicPartition>> assigned) {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bs);
        p.put(ConsumerConfig.GROUP_ID_CONFIG, topic + "-g");
        p.put(ConsumerConfig.GROUP_PROTOCOL_CONFIG, "consumer");
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        try (KafkaConsumer<String, String> c = new KafkaConsumer<>(p)) {
            c.subscribe(List.of(topic));
            while (done.get() < n && !stop.get()) {
                for (ConsumerRecord<String, String> r : c.poll(Duration.ofMillis(100))) {
                    firstAt.compareAndSet(0, System.nanoTime());
                    sleep(1);                                    // "xu ly" 1 ms: goi DB, goi HTTP...
                    processed.get(name).incrementAndGet();
                    if (done.incrementAndGet() == n) lastAt.set(System.nanoTime());
                }
                assigned.put(name, Set.copyOf(c.assignment()));
            }
        }
    }

    static void produce(String bs, String topic, int n) {
        Properties pp = new Properties();
        pp.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bs);
        pp.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        pp.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        try (KafkaProducer<String, String> p = new KafkaProducer<>(pp)) {
            for (int i = 0; i < n; i++) p.send(new ProducerRecord<>(topic, "k" + i, Integer.toString(i)));
        }
    }

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
