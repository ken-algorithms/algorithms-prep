package com.prep.cs.kafka;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.RangeAssignor;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

/**
 * Ba cach consumer group chia lai partition khi co thanh vien vao/ra, tren cung mot kich ban:
 *   c1 vao -> c2 vao -> c3 vao -> c2 roi nhom.
 *   eager       : classic protocol + RangeAssignor. Moi lan chia lai, MOI thanh vien tra HET partition.
 *   cooperative : classic protocol + CooperativeStickyAssignor. Chi tra partition phai chuyen chu.
 *   consumer    : protocol moi KIP-848 (group.protocol=consumer). Broker tu tinh phan chia, khong co rao chan chung.
 * In tung su kien voi moc thoi gian, va cuoi cung: partition nao bi DUNG ma van ve dung chu cu (dung vo ich).
 *
 *   scripts/run.sh com.prep.cs.kafka.GroupRebalanceDemo localhost:19092 orders6 eager
 */
public final class GroupRebalanceDemo {

    private static long t0;
    private static final Object LOCK = new Object();
    // partition -> [luc bi tra (ms), chu cu]
    private static final Map<Integer, long[]> revokedAt = new HashMap<>();
    private static final Map<Integer, String> revokedFrom = new HashMap<>();
    private static final List<String> gaps = new ArrayList<>();

    public static void main(String[] a) throws Exception {
        String bootstrap = a[0], topic = a[1], mode = a[2];
        String group = "rebalance-" + mode + "-" + System.currentTimeMillis() % 100_000;
        t0 = System.currentTimeMillis();
        log("== che do " + mode + ", group " + group);
        Member c1 = start(bootstrap, topic, group, mode, "c1");
        Thread.sleep(6_000);
        log("-- c2 tham gia");
        Member c2 = start(bootstrap, topic, group, mode, "c2");
        Thread.sleep(8_000);
        log("-- c3 tham gia");
        Member c3 = start(bootstrap, topic, group, mode, "c3");
        Thread.sleep(8_000);
        log("-- c2 roi nhom (close)");
        c2.stop();
        Thread.sleep(8_000);
        log("-- ket thuc");
        c1.stop();
        c3.stop();
        synchronized (LOCK) {
            System.out.println("Partition bi tra roi nhan lai, kem khoang khong co chu:");
            gaps.forEach(System.out::println);
        }
    }

    static Member start(String bootstrap, String topic, String group, String mode, String name) {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ConsumerConfig.GROUP_ID_CONFIG, group);
        p.put(ConsumerConfig.CLIENT_ID_CONFIG, name);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        switch (mode) {
            case "eager" -> {
                p.put(ConsumerConfig.GROUP_PROTOCOL_CONFIG, "classic");
                p.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, RangeAssignor.class.getName());
            }
            case "cooperative" -> {
                p.put(ConsumerConfig.GROUP_PROTOCOL_CONFIG, "classic");
                p.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, CooperativeStickyAssignor.class.getName());
            }
            // KIP-848: KHONG duoc dat partition.assignment.strategy, session/heartbeat timeout o client.
            case "consumer" -> p.put(ConsumerConfig.GROUP_PROTOCOL_CONFIG, "consumer");
            default -> throw new IllegalArgumentException(mode);
        }
        Member m = new Member(name, new KafkaConsumer<>(p), topic);
        Thread th = new Thread(m, name);
        th.start();
        m.thread = th;
        return m;
    }

    static final class Member implements Runnable {
        final String name;
        final KafkaConsumer<String, String> consumer;
        final String topic;
        final TreeSet<Integer> owned = new TreeSet<>();
        volatile boolean running = true;
        Thread thread;

        Member(String name, KafkaConsumer<String, String> consumer, String topic) {
            this.name = name;
            this.consumer = consumer;
            this.topic = topic;
        }

        @Override
        public void run() {
            consumer.subscribe(List.of(topic), new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> ps) {
                    if (ps.isEmpty()) return;
                    synchronized (LOCK) {
                        long now = System.currentTimeMillis() - t0;
                        for (TopicPartition tp : ps) {
                            revokedAt.put(tp.partition(), new long[]{now});
                            revokedFrom.put(tp.partition(), name);
                            owned.remove(tp.partition());
                        }
                        log(name + " tra lai " + parts(ps) + " -> con " + owned);
                    }
                }

                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> ps) {
                    if (ps.isEmpty()) return;
                    synchronized (LOCK) {
                        long now = System.currentTimeMillis() - t0;
                        for (TopicPartition tp : ps) {
                            owned.add(tp.partition());
                            long[] r = revokedAt.remove(tp.partition());
                            if (r != null) {
                                String from = revokedFrom.remove(tp.partition());
                                gaps.add(String.format("  p%d: %s -> %s, khong co chu %d ms%s", tp.partition(), from, name,
                                        now - r[0], from.equals(name) ? "  (ve DUNG chu cu: dung vo ich)" : ""));
                            }
                        }
                        log(name + " nhan " + parts(ps) + " -> giu " + owned);
                    }
                }

                @Override
                public void onPartitionsLost(Collection<TopicPartition> ps) {
                    log(name + " MAT " + parts(ps));
                }
            });
            while (running) {
                consumer.poll(Duration.ofMillis(100));
            }
            consumer.close(); // gui LeaveGroup / heartbeat epoch -1: broker chia lai ngay, khong doi session timeout
        }

        void stop() throws InterruptedException {
            running = false;
            thread.join();
        }
    }

    static String parts(Collection<TopicPartition> ps) {
        return ps.stream().map(TopicPartition::partition).sorted().map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }

    static void log(String s) {
        synchronized (LOCK) {
            System.out.printf("+%6.3fs %s%n", (System.currentTimeMillis() - t0) / 1000.0, s);
        }
    }
}
