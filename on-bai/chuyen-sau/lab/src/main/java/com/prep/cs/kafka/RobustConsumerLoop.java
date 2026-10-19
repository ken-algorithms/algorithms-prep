package com.prep.cs.kafka;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

/**
 * Vong consumer at-least-once "dung sach giao khoa" (drill bai 04): xu ly roi moi commit, commit CO DIEU KIEN theo
 * tung partition, commit phan dang do khi bi thu hoi partition, tat em bang wakeup() tu luong khac.
 *
 *   scripts/run.sh com.prep.cs.kafka.RobustConsumerLoop localhost:19092 orders6 g-robust 10
 */
public final class RobustConsumerLoop {

    private final KafkaConsumer<String, String> consumer;
    private final Map<TopicPartition, OffsetAndMetadata> pending = new HashMap<>();  // da xu ly, chua commit
    private long processed;

    RobustConsumerLoop(String bootstrap, String group) {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ConsumerConfig.GROUP_ID_CONFIG, group);
        p.put(ConsumerConfig.GROUP_PROTOCOL_CONFIG, "consumer");           // KIP-848
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);           // tu commit SAU khi xu ly
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 200);               // vua voi thoi gian xu ly mot vong
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        this.consumer = new KafkaConsumer<>(p);
    }

    void run(String topic) {
        consumer.subscribe(List.of(topic), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> parts) {
                commitPending();                         // giao lai partition voi offset dung: it trung nhat
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> parts) {
                // offset da commit duoc consumer tu nap; neu luu offset o DB (bai 08) thi seek o day
            }
        });
        try {
            while (true) {
                for (ConsumerRecord<String, String> r : consumer.poll(Duration.ofMillis(500))) {
                    handle(r);                                                  // phai idempotent: at-least-once
                    pending.put(new TopicPartition(r.topic(), r.partition()),
                            new OffsetAndMetadata(r.offset() + 1));            // commit offset KE TIEP
                }
                commitPending();
            }
        } catch (WakeupException e) {
            // tu luong khac goi consumer.wakeup(): thoat vong de tat em
        } finally {
            try {
                commitPending();
            } finally {
                consumer.close();                        // roi group ngay, partition giao cho nguoi khac lien
            }
        }
    }

    private void handle(ConsumerRecord<String, String> r) {
        processed++;
    }

    private void commitPending() {
        if (pending.isEmpty()) return;
        consumer.commitSync(pending);                    // chi nhung partition minh dang giu
        pending.clear();
    }

    public static void main(String[] a) throws Exception {
        RobustConsumerLoop loop = new RobustConsumerLoop(a[0], a[2]);
        Thread worker = new Thread(() -> loop.run(a[1]), "consumer");
        worker.start();
        Thread.sleep(Long.parseLong(a[3]) * 1_000);
        loop.consumer.wakeup();                          // KafkaConsumer khong thread-safe, tru wakeup()
        worker.join();
        System.out.printf("da xu ly %d record, commit xong, roi group%n", loop.processed);
    }
}
