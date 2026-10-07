package com.prep.perf.demo;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.RebalanceInProgressException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * P20 - Kafka consumer goi downstream DONG BO cho tung message. Can broker that.
 *
 * <pre>
 * XAU   @KafkaListener void on(Event e) { notificationClient.send(e); }    // 20 ms moi message
 *       max.poll.records = 500 -> 500 x 20 ms = 10 s cho MOT lan poll
 *       vuot max.poll.interval.ms -> broker coi consumer da chet -> rebalance -> commit that bai
 *       -> consumer khac (hoac chinh no) doc lai tu offset cu -> lai 10 s -> lai rebalance
 * SUA A giam max.poll.records xuong 50 (1 s moi poll) - het rebalance, nhung van cham
 * SUA B goi downstream THEO LO: mot lan goi cho ca batch (20 ms + 1 ms/message)
 * </pre>
 *
 * max.poll.interval.ms o day dat 6 s (mac dinh 300 s) de demo chay trong vai chuc giay. Ty le
 * "thoi gian xu ly mot poll / max.poll.interval.ms" moi la thu quyet dinh, khong phai con so tuyet doi:
 * production voi mac dinh 300 s va 500 record se vo khi downstream cham hon 600 ms moi message.
 *
 * <pre>java -cp target/benchmarks.jar com.prep.perf.demo.D20SlowKafkaConsumer [bootstrap=localhost:9092]</pre>
 */
public final class D20SlowKafkaConsumer {

    private D20SlowKafkaConsumer() {}

    enum Mode { PER_MESSAGE_500, PER_MESSAGE_50, BATCH_CALL_500 }

    record Result(String mode, int messages, int distinctProcessed, long totalProcessed, long duplicates,
                  int partitionsRevokedOrLost, int commitFailures, long wallMs, boolean finished) {}

    static final int MESSAGES = 2_000;
    static final int PARTITIONS = 4;
    static final int CONSUMERS = 2;
    static final int POLL_INTERVAL_MS = 6_000;
    static final long PER_MESSAGE_MS = 20;
    static final long DEADLINE_MS = 45_000;

    public static void main(String[] args) throws Exception {
        String bootstrap = args.length > 0 ? args[0] : "localhost:9092";
        System.out.printf("%d messages, %d partitions, %d consumers, downstream %d ms/message, max.poll.interval.ms=%d%n",
                MESSAGES, PARTITIONS, CONSUMERS, PER_MESSAGE_MS, POLL_INTERVAL_MS);
        for (Mode m : Mode.values()) {
            System.out.println(run(bootstrap, m));
        }
    }

    static Result run(String bootstrap, Mode mode) throws Exception {
        String topic = "transfer.completed." + mode.name().toLowerCase() + "." + System.nanoTime();
        createTopic(bootstrap, topic);

        var seen = new ConcurrentHashMap<String, AtomicInteger>();
        var total = new LongAdder();
        var revocations = new AtomicInteger();
        var commitFailures = new AtomicInteger();
        var stop = new AtomicBoolean();
        var armed = new AtomicBoolean();                       // chi dem rebalance sau khi group on dinh
        var assigned = new ConcurrentHashMap<String, Collection<TopicPartition>>();
        String group = "notification-" + mode.name().toLowerCase() + "-" + System.nanoTime();

        List<Thread> threads = new ArrayList<>();
        for (int c = 0; c < CONSUMERS; c++) {
            String name = "consumer-" + c;
            Thread t = Thread.ofPlatform().name(name).start(() -> consume(bootstrap, topic, group, mode, name,
                    seen, total, revocations, commitFailures, stop, armed, assigned));
            threads.add(t);
        }
        // cho ca hai consumer cung co partition (group on dinh) roi moi day message vao
        while (assigned.size() < CONSUMERS || assigned.values().stream().mapToInt(Collection::size).sum() < PARTITIONS) {
            Thread.sleep(50);
        }
        armed.set(true);
        long start = System.nanoTime();
        produce(bootstrap, topic);
        while (seen.size() < MESSAGES && TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < DEADLINE_MS) {
            Thread.sleep(100);
        }
        long wall = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        stop.set(true);
        for (Thread t : threads) {
            t.join(30_000);
        }
        long totalProcessed = total.sum();
        return new Result(mode.name(), MESSAGES, seen.size(), totalProcessed, totalProcessed - seen.size(),
                revocations.get(), commitFailures.get(), wall, seen.size() == MESSAGES);
    }

    private static void consume(String bootstrap, String topic, String group, Mode mode, String name,
                                Map<String, AtomicInteger> seen, LongAdder total, AtomicInteger revocations,
                                AtomicInteger commitFailures, AtomicBoolean stop, AtomicBoolean armed,
                                Map<String, Collection<TopicPartition>> assigned) {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ConsumerConfig.GROUP_ID_CONFIG, group);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        p.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, String.valueOf(POLL_INTERVAL_MS));
        p.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, mode == Mode.PER_MESSAGE_50 ? "50" : "500");
        p.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "6000");
        p.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, "1000");

        try (var consumer = new KafkaConsumer<String, String>(p)) {
            consumer.subscribe(List.of(topic), new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> parts) {
                    if (!parts.isEmpty() && armed.get() && !stop.get()) {
                        revocations.incrementAndGet();
                    }
                }

                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> parts) {
                    assigned.put(name, List.copyOf(parts));
                }

                @Override
                public void onPartitionsLost(Collection<TopicPartition> parts) {
                    if (!parts.isEmpty() && armed.get() && !stop.get()) {
                        revocations.incrementAndGet();   // bi duoi khoi group vi qua max.poll.interval
                    }
                }
            });
            while (!stop.get()) {
                ConsumerRecords<String, String> batch = consumer.poll(Duration.ofMillis(200));
                if (batch.isEmpty()) {
                    continue;
                }
                if (mode == Mode.BATCH_CALL_500) {
                    Lat.sleepMs(PER_MESSAGE_MS + batch.count());   // mot lan goi API theo lo
                    for (ConsumerRecord<String, String> r : batch) {
                        record(seen, total, r.value());
                    }
                } else {
                    for (ConsumerRecord<String, String> r : batch) {
                        if (stop.get()) {
                            break;
                        }
                        Lat.sleepMs(PER_MESSAGE_MS);              // goi HTTP dong bo cho MOI message
                        record(seen, total, r.value());
                    }
                }
                try {
                    consumer.commitSync();
                } catch (CommitFailedException | RebalanceInProgressException e) {
                    commitFailures.incrementAndGet();             // batch nay se bi xu ly LAI
                }
            }
        }
    }

    private static void record(Map<String, AtomicInteger> seen, LongAdder total, String eventId) {
        total.increment();
        seen.computeIfAbsent(eventId, k -> new AtomicInteger()).incrementAndGet();
    }

    private static void createTopic(String bootstrap, String topic) throws Exception {
        try (Admin admin = Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap))) {
            admin.createTopics(List.of(new NewTopic(topic, PARTITIONS, (short) 1))).all().get(30, TimeUnit.SECONDS);
        }
    }

    private static void produce(String bootstrap, String topic) throws Exception {
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.ACKS_CONFIG, "all");
        try (var producer = new KafkaProducer<String, String>(p)) {
            for (int i = 0; i < MESSAGES; i++) {
                producer.send(new ProducerRecord<>(topic, "acct-" + (i % 50), "evt-" + i));
            }
            producer.flush();
        }
    }
}
