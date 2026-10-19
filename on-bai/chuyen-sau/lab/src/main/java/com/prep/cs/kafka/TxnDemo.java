package com.prep.cs.kafka;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
 * Transaction cua Kafka, ba thi nghiem tren mot partition:
 *   1. abort roi commit: read_uncommitted thay ca hai, read_committed chi thay phan commit; offset bi "thung"
 *      vi marker COMMIT/ABORT chiem offset.
 *   2. LSO: mot transaction dang mo CHAN read_committed doc qua no, ke ca record khong-transaction ghi sau.
 *   3. Zombie fencing: producer thu hai cung transactional.id -> producer cu bi chan ngay o lan ghi ke tiep.
 *
 *   scripts/run.sh com.prep.cs.kafka.TxnDemo localhost:19092
 */
public final class TxnDemo {

    public static void main(String[] a) throws Exception {
        String bs = a[0];
        String topic = "txn-demo-" + System.currentTimeMillis() % 100_000;
        try (Admin admin = Admin.create(Map.of("bootstrap.servers", bs))) {
            admin.createTopics(List.of(new NewTopic(topic, 1, (short) 3))).all().get();
        }
        System.out.println("topic " + topic);

        System.out.println("\n--- 1. Transaction abort roi transaction commit");
        try (KafkaProducer<String, String> tx = txProducer(bs, "demo-tx-1")) {
            tx.initTransactions();
            tx.beginTransaction();
            for (String v : List.of("a1", "a2", "a3")) tx.send(new ProducerRecord<>(topic, v));
            tx.flush();            // dua ca ba xuong broker; khong flush thi abort bo luon record con nam trong buffer
            tx.abortTransaction();
            tx.beginTransaction();
            for (String v : List.of("c1", "c2", "c3")) tx.send(new ProducerRecord<>(topic, v));
            tx.commitTransaction();
        }
        System.out.println("read_uncommitted: " + readAll(bs, topic, "read_uncommitted", 3_000));
        System.out.println("read_committed  : " + readAll(bs, topic, "read_committed", 3_000));

        System.out.println("\n--- 2. Transaction dang mo chan read_committed (LSO)");
        try (KafkaProducer<String, String> open = txProducer(bs, "demo-tx-2");
             KafkaProducer<String, String> plain = plainProducer(bs)) {
            open.initTransactions();
            open.beginTransaction();
            open.send(new ProducerRecord<>(topic, "pending")).get();
            plain.send(new ProducerRecord<>(topic, "plain-after")).get(); // KHONG thuoc transaction nao
            System.out.println("dang mo transaction:");
            System.out.println("  read_uncommitted: " + readAll(bs, topic, "read_uncommitted", 3_000));
            System.out.println("  read_committed  : " + readAll(bs, topic, "read_committed", 3_000)
                    + "   <- khong thay ca 'plain-after'");
            System.out.println("  end offset (HW) = " + endOffset(bs, topic, "read_uncommitted")
                    + ", LSO = " + endOffset(bs, topic, "read_committed"));
            open.commitTransaction();
            System.out.println("sau commit:");
            System.out.println("  read_committed  : " + readAll(bs, topic, "read_committed", 3_000));
        }

        System.out.println("\n--- 3. Zombie fencing: hai producer cung transactional.id");
        KafkaProducer<String, String> zombie = txProducer(bs, "demo-tx-3");
        zombie.initTransactions();
        zombie.beginTransaction();
        zombie.send(new ProducerRecord<>(topic, "zombie-1")).get();
        System.out.println("zombie: dang trong transaction, da ghi zombie-1 (chua commit)");
        try (KafkaProducer<String, String> fresh = txProducer(bs, "demo-tx-3")) {
            fresh.initTransactions(); // tang epoch, abort transaction dang mo cua zombie
            System.out.println("fresh : initTransactions() xong -> epoch moi, transaction cua zombie bi abort");
            try {
                zombie.send(new ProducerRecord<>(topic, "zombie-2")).get();
                zombie.commitTransaction();
                System.out.println("zombie: commit THANH CONG (sai!)");
            } catch (Exception e) {
                Throwable c = e.getCause() != null ? e.getCause() : e;
                System.out.println("zombie: bi chan -> " + c.getClass().getSimpleName() + ": " + c.getMessage());
            }
            fresh.beginTransaction();
            fresh.send(new ProducerRecord<>(topic, "fresh-1"));
            fresh.commitTransaction();
        }
        try {
            zombie.close(Duration.ofSeconds(1));
        } catch (Exception ignored) {
            // producer da bi fence: close co the nem loi, khong quan trong
        }
        System.out.println("read_committed  : " + readAll(bs, topic, "read_committed", 3_000));
    }

    static KafkaProducer<String, String> txProducer(String bs, String txId) {
        Properties p = base(bs);
        p.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, txId);   // bat idempotence + acks=all theo
        p.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, 30_000);
        return new KafkaProducer<>(p);
    }

    static KafkaProducer<String, String> plainProducer(String bs) {
        return new KafkaProducer<>(base(bs));
    }

    static Properties base(String bs) {
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bs);
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return p;
    }

    /** Doc tu dau partition 0 trong mot khoang thoi gian; in "offset:value". */
    static List<String> readAll(String bs, String topic, String isolation, long millis) {
        List<String> out = new ArrayList<>();
        try (KafkaConsumer<String, String> c = consumer(bs, isolation)) {
            TopicPartition tp = new TopicPartition(topic, 0);
            c.assign(List.of(tp));
            c.seekToBeginning(List.of(tp));
            long until = System.currentTimeMillis() + millis;
            while (System.currentTimeMillis() < until) {
                for (ConsumerRecord<String, String> r : c.poll(Duration.ofMillis(200))) {
                    out.add(r.offset() + ":" + r.value());
                }
            }
        }
        return out;
    }

    static long endOffset(String bs, String topic, String isolation) {
        try (KafkaConsumer<String, String> c = consumer(bs, isolation)) {
            TopicPartition tp = new TopicPartition(topic, 0);
            return c.endOffsets(List.of(tp)).get(tp);   // read_committed: tra ve LSO, khong phai HW
        }
    }

    static KafkaConsumer<String, String> consumer(String bs, String isolation) {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bs);
        p.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, isolation);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return new KafkaConsumer<>(p);
    }
}
