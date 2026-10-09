package com.prep.kafka.producer;

import com.prep.kafka.model.EventBatch;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class EventProducer {

    public enum KeyStrategy {
        TENANT_ID,
        BATCH_ID
    }

    private final KafkaTemplate<String, EventBatch> kafkaTemplate;

    public EventProducer(KafkaTemplate<String, EventBatch> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<RecordMetadata> send(String topic, EventBatch batch, KeyStrategy strategy) {
        String key = (strategy == KeyStrategy.TENANT_ID) ? batch.tenantId() : batch.batchId();
        ProducerRecord<String, EventBatch> record = new ProducerRecord<>(topic, key, batch);
        record.headers().add("batch-id", batch.batchId().getBytes());
        record.headers().add("tenant-id", batch.tenantId().getBytes());

        return kafkaTemplate.send(record).thenApply(result -> result.getRecordMetadata());
    }

    public CompletableFuture<RecordMetadata> sendRaw(String topic, String key, Object value) {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> rawTemplate = (KafkaTemplate<String, Object>) (KafkaTemplate<?, ?>) kafkaTemplate;
        return rawTemplate.send(topic, key, value).thenApply(result -> result.getRecordMetadata());
    }
}
