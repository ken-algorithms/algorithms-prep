package com.prep.cs.kafka;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

/**
 * Doi chieu: moi id producer DA NHAN XAC NHAN co nam trong topic khong. "Mat" = da xac nhan ma khong con.
 * Doc thang tu dau moi partition (assign + seekToBeginning, khong dung consumer group).
 *
 *   scripts/run.sh com.prep.cs.kafka.AckLossCheck localhost:19092 pay results/acked-pay.txt
 */
public final class AckLossCheck {

    public static void main(String[] a) throws Exception {
        String bootstrap = a[0], topic = a[1];
        Set<Long> acked = Files.readAllLines(Path.of(a[2])).stream().filter(s -> !s.isBlank())
                .map(Long::parseLong).collect(Collectors.toSet());

        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        Map<Long, Integer> seen = new HashMap<>();
        try (KafkaConsumer<String, String> c = new KafkaConsumer<>(p)) {
            List<TopicPartition> tps = c.partitionsFor(topic).stream()
                    .map(pi -> new TopicPartition(topic, pi.partition())).toList();
            c.assign(tps);
            c.seekToBeginning(tps);
            Map<TopicPartition, Long> end = c.endOffsets(tps);
            while (tps.stream().anyMatch(tp -> c.position(tp) < end.get(tp))) {
                for (ConsumerRecord<String, String> r : c.poll(Duration.ofMillis(500))) {
                    seen.merge(Long.parseLong(r.value()), 1, Integer::sum);
                }
            }
        }
        Set<Long> lost = new HashSet<>(acked);
        lost.removeAll(seen.keySet());
        long dup = seen.values().stream().filter(n -> n > 1).count();
        long notAckedButPresent = seen.keySet().stream().filter(id -> !acked.contains(id)).count();
        System.out.printf("topic %s: %d id da xac nhan, %d id co trong log%n", topic, acked.size(), seen.size());
        System.out.printf("MAT (da xac nhan ma khong con trong log): %d%s%n", lost.size(),
                lost.isEmpty() ? "" : "  vi du " + lost.stream().sorted().limit(5).toList()
                        + " .. " + lost.stream().max(Long::compare).orElseThrow());
        System.out.printf("trung (mot id xuat hien >1 lan): %d; co trong log nhung producer khong nhan xac nhan: %d%n",
                dup, notAckedButPresent);
    }
}
