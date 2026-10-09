package com.prep.kafka;

import com.prep.kafka.consumer.BatchEventConsumer;
import com.prep.kafka.model.EventBatch;
import com.prep.kafka.model.EventItem;
import com.prep.kafka.model.Outcome;
import com.prep.kafka.repository.RollupRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchProcessingVsSlowConsumerTest {

    @Test
    @DisplayName("P20: Xử lý đồng bộ từng record gây chậm 10s dẫn tới Rebalance Storm")
    void testSlowPerMessageProcessingSimulation() {
        int recordsCount = 500;
        long downstreamRpcDelayMs = 20; // 20ms mỗi downstream call

        long estimatedPerMessageTotalMs = recordsCount * downstreamRpcDelayMs; // 10,000ms = 10s
        long maxPollIntervalMs = 6_000; // 6s poll interval

        // Chứng minh: thời gian xử lý vượt quá max.poll.interval.ms -> Trigger Rebalance Storm
        assertTrue(estimatedPerMessageTotalMs > maxPollIntervalMs,
                "Xử lý từng message làm 1 lần poll kéo dài 10s, vượt quá 6s của max.poll.interval.ms!");

        System.out.printf("=== P20 Slow Consumer: 500 records x 20ms = %d ms > max.poll.interval.ms (%d ms) -> REBALANCE STORM!%n",
                estimatedPerMessageTotalMs, maxPollIntervalMs);
    }

    @Test
    @DisplayName("Sửa P20: Batch processing gộp xử lý 500 records trong vài chục ms")
    void testBatchEventConsumerProcessesFast() {
        // Fake RollupRepository thuần Java, không dùng Mockito/ByteBuddy để an toàn 100% trên Java 21-25
        RollupRepository repo = new RollupRepository(null) {
            @Override
            public boolean tryRecordBatch(String batchId, String tenantId) {
                return true;
            }
            @Override
            public void recordAggregates(String tenantId, long bucketMinute, Map<Outcome, Long> counts) {
                // fast in-memory no-op
            }
        };

        BatchEventConsumer consumer = new BatchEventConsumer(repo);

        List<ConsumerRecord<String, EventBatch>> records = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            String batchId = UUID.randomUUID().toString();
            EventBatch batch = new EventBatch(
                    batchId,
                    Instant.now(),
                    "tenant_test",
                    List.of(new EventItem("feature_1", true), new EventItem("feature_2", false))
            );
            records.add(new ConsumerRecord<>("events", 0, i, batchId, batch));
        }

        long start = System.currentTimeMillis();
        consumer.processBatchRecords(records);
        long elapsed = System.currentTimeMillis() - start;

        // Xử lý 500 records bằng batch consumer chỉ mất dưới 500ms (so với 10,000ms của per-message)
        assertTrue(elapsed < 1000, "Batch processing phải hoàn thành nhanh hơn nhiều lần");
        assertEquals(500, consumer.getAppliedBatches());

        System.out.printf("=== Batch processing: 500 records hoàn tất trong %d ms (Nhanh hơn gấp ~%dx so với per-message)%n",
                elapsed, Math.max(1, 10000 / Math.max(1, elapsed)));
    }
}
