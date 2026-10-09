package com.prep.kafka.resilience;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Cau hinh co che co lap "Poison Pill" (message loi payload / sai dinh dang):
 * - Dung DefaultErrorHandler voi DeadLetterPublishingRecoverer
 * - Retry 2 lan cach nhau 100ms
 * - Neu van loi: day sang topic <original-topic>.DLT
 * - Khong lam nghen consumer group (tranh loi Head-of-Line Blocking)
 */
@Configuration
public class PoisonPillConfig {

    public static final String DLT_SUFFIX = ".DLT";

    @Bean
    public CommonErrorHandler errorHandler(KafkaOperations<Object, Object> operations) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                operations,
                (record, exception) -> new TopicPartition(record.topic() + DLT_SUFFIX, record.partition())
        );

        // Retry 2 lan voi interval 100ms roi moi route sang DLT
        FixedBackOff backOff = new FixedBackOff(100L, 2);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // Khong retry cac loi logic khong the cuu van (vi du DeserializationException)
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);

        return errorHandler;
    }
}
