package com.prep.cs.kafka;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * Commit truoc hay xu ly truoc quyet dinh at-least-once hay at-most-once. Thi nghiem bang CRASH THAT:
 * tien trinh con xu ly record (ghi id ra file), cha kill -9 no giua mot batch, roi cho tien trinh con thu hai
 * doc tiep tu offset da commit. Dem: id nao xu ly hai lan (trung), id nao khong bao gio duoc xu ly (mat).
 *
 *   scripts/run.sh com.prep.cs.kafka.CommitSemantics localhost:19092 at-least-once [kill-sau-N-record]
 *   mode: at-least-once (xu ly roi commit) | at-most-once (commit roi xu ly) | auto (enable.auto.commit)
 */
public final class CommitSemantics {

    static final int RECORDS = 1_000;

    public static void main(String[] a) throws Exception {
        if (a.length > 0 && a[0].equals("child")) {
            child(a[1], a[2], a[3], a[4], Path.of(a[5]));
            return;
        }
        String bootstrap = a[0], mode = a[1];
        int crashAfter = a.length > 2 ? Integer.parseInt(a[2]) : 350;
        String topic = "commit-" + mode + "-" + System.currentTimeMillis() % 100_000;
        String group = "g-" + topic;
        try (Admin admin = Admin.create(Map.of("bootstrap.servers", bootstrap))) {
            admin.createTopics(List.of(new NewTopic(topic, 1, (short) 3))).all().get();
        }
        produce(bootstrap, topic);
        Path processed = Files.createTempFile("processed-", ".txt");

        Process first = spawn(bootstrap, topic, group, mode, processed);
        while (lines(processed) < crashAfter) Thread.sleep(5);
        first.destroyForcibly().waitFor(); // SIGKILL: khong close(), khong commit, khong LeaveGroup
        int atCrash = lines(processed);
        System.out.printf("[%s] kill -9 tien trinh 1 sau khi no xu ly %d record%n", mode, atCrash);

        Process second = spawn(bootstrap, topic, group, mode, processed);
        if (!second.waitFor(90, TimeUnit.SECONDS)) throw new IllegalStateException("tien trinh 2 khong xong");

        Map<Integer, Integer> times = new HashMap<>();
        for (String s : Files.readAllLines(processed)) times.merge(Integer.parseInt(s.trim()), 1, Integer::sum);
        long dup = times.values().stream().filter(n -> n > 1).count();
        long missing = RECORDS - times.size();
        System.out.printf("[%s] xu ly tong %d lan cho %d record: TRUNG %d, MAT %d%n",
                mode, times.values().stream().mapToInt(Integer::intValue).sum(), RECORDS, dup, missing);
    }

    static void child(String bootstrap, String topic, String group, String mode, Path processed) throws IOException {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ConsumerConfig.GROUP_ID_CONFIG, group);
        p.put(ConsumerConfig.GROUP_PROTOCOL_CONFIG, "classic");
        p.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 6_000);   // thanh vien chet bi loai sau 6 giay
        p.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 2_000);
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, mode.equals("auto"));
        p.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1_000);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        TopicPartition tp = new TopicPartition(topic, 0);
        try (KafkaConsumer<String, String> c = new KafkaConsumer<>(p)) {
            c.subscribe(List.of(topic));
            while (true) {
                ConsumerRecords<String, String> batch = c.poll(Duration.ofMillis(200));
                if (mode.equals("at-most-once") && !batch.isEmpty()) c.commitSync();  // commit TRUOC
                for (ConsumerRecord<String, String> r : batch) {
                    Files.writeString(processed, r.value() + "\n", StandardOpenOption.APPEND); // "xu ly"
                    sleepQuietly(2);
                }
                if (mode.equals("at-least-once") && !batch.isEmpty()) c.commitSync(); // commit SAU
                if (!c.assignment().isEmpty() && c.position(tp) >= RECORDS) {
                    c.commitSync();
                    return;
                }
            }
        }
    }

    static Process spawn(String bootstrap, String topic, String group, String mode, Path processed) throws IOException {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        return new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"), CommitSemantics.class.getName(),
                "child", bootstrap, topic, group, mode, processed.toString()).inheritIO().start();
    }

    static void produce(String bootstrap, String topic) throws Exception {
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(p)) {
            for (int i = 0; i < RECORDS; i++) producer.send(new ProducerRecord<>(topic, Integer.toString(i)));
        }
    }

    static int lines(Path f) throws IOException {
        return (int) Files.readAllLines(f).stream().filter(s -> !s.isBlank()).count();
    }

    static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
