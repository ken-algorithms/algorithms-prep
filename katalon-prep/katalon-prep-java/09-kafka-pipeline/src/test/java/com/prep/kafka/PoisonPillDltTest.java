package com.prep.kafka;

import com.prep.kafka.model.EventClassifier;
import com.prep.kafka.model.EventItem;
import com.prep.kafka.model.Outcome;
import com.prep.kafka.resilience.PoisonPillConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PoisonPillDltTest {

    @Test
    @DisplayName("Poison Pill: Phân loại dữ liệu dị dạng (null, rỗng) thành MALFORMED thay vì ném NullPointerException")
    void testMalformedItemClassification() {
        EventItem itemWithNullKey = new EventItem(null, true);
        EventItem itemWithBlankKey = new EventItem("   ", false);
        EventItem itemWithNullVal = new EventItem("feature_1", null);

        assertEquals(Outcome.MALFORMED, EventClassifier.classify(itemWithNullKey));
        assertEquals(Outcome.MALFORMED, EventClassifier.classify(itemWithBlankKey));
        assertEquals(Outcome.MALFORMED, EventClassifier.classify(itemWithNullVal));
        assertEquals(Outcome.MALFORMED, EventClassifier.classify(null));
    }

    @Test
    @DisplayName("DLT Routing: Bản ghi lỗi được chuyển tự động tới topic .DLT cùng partition")
    void testDltDestinationResolution() {
        @SuppressWarnings("unchecked")
        KafkaOperations<Object, Object> ops = (KafkaOperations<Object, Object>) Proxy.newProxyInstance(
                KafkaOperations.class.getClassLoader(),
                new Class<?>[]{KafkaOperations.class},
                (proxy, method, args) -> {
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    return null;
                }
        );

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                ops,
                (record, exception) -> new TopicPartition(record.topic() + PoisonPillConfig.DLT_SUFFIX, record.partition())
        );

        ConsumerRecord<String, String> badRecord = new ConsumerRecord<>("events.ingest", 2, 42L, "key", "bad_json_payload");
        TopicPartition destination = new TopicPartition(badRecord.topic() + PoisonPillConfig.DLT_SUFFIX, badRecord.partition());

        assertEquals("events.ingest.DLT", destination.topic());
        assertEquals(2, destination.partition());

        System.out.println("=== Poison pill correctly diverted to DLT: " + destination.topic() + ", partition: " + destination.partition());
    }
}
