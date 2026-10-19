package com.prep.cs.kafka;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

/**
 * In gia tri mac dinh THAT cua kafka-clients dang dung (khong tin tai lieu cu, mac dinh doi qua cac ban).
 *
 *   scripts/run.sh com.prep.cs.kafka.ClientDefaults
 */
public final class ClientDefaults {

    public static void main(String[] a) {
        Map<String, Object> p = ProducerConfig.configDef().defaultValues();
        Map<String, Object> c = ConsumerConfig.configDef().defaultValues();
        System.out.println("kafka-clients " + org.apache.kafka.common.utils.AppInfoParser.getVersion());
        for (String k : new String[]{"acks", "enable.idempotence", "max.in.flight.requests.per.connection", "retries",
                "linger.ms", "batch.size", "buffer.memory", "max.block.ms", "request.timeout.ms",
                "delivery.timeout.ms", "compression.type", "partitioner.ignore.keys"}) {
            System.out.printf("producer  %-40s = %s%n", k, p.get(k));
        }
        for (String k : new String[]{"group.protocol", "partition.assignment.strategy", "enable.auto.commit",
                "auto.commit.interval.ms", "auto.offset.reset", "isolation.level", "max.poll.records",
                "max.poll.interval.ms", "session.timeout.ms", "heartbeat.interval.ms", "fetch.min.bytes",
                "fetch.max.wait.ms", "max.partition.fetch.bytes"}) {
            Object v = c.get(k);
            if (v instanceof java.util.List<?> l) {
                v = l.stream().map(x -> x.toString().replace("class org.apache.kafka.clients.consumer.", "")).toList();
            }
            System.out.printf("consumer  %-40s = %s%n", k, v);
        }
    }
}
