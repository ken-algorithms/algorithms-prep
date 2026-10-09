package com.prep.kafka.consumer;

import com.prep.kafka.model.EventBatch;
import com.prep.kafka.model.EventClassifier;
import com.prep.kafka.model.EventItem;
import com.prep.kafka.model.Outcome;
import com.prep.kafka.repository.RollupRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Consumer xu ly THEO LO (Batch Processing):
 * - Nhan List<ConsumerRecord> thay vi tung record don le
 * - Giai quyet dut diem loi P20 (Consumer cham dan den Rebalance Storm)
 * - Tich hop Idempotent Deduplication va Aggregation theo phut
 */
@Service
public class BatchEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(BatchEventConsumer.class);

    private final RollupRepository rollupRepo;
    private final AtomicInteger appliedBatches = new AtomicInteger();
    private final AtomicInteger skippedDuplicates = new AtomicInteger();
    private final AtomicLong totalEventsProcessed = new AtomicLong();

    public BatchEventConsumer(RollupRepository rollupRepo) {
        this.rollupRepo = rollupRepo;
    }

    public void processBatchRecords(List<ConsumerRecord<String, EventBatch>> records) {
        long start = System.currentTimeMillis();
        int batchApplied = 0;
        int batchSkipped = 0;

        for (ConsumerRecord<String, EventBatch> record : records) {
            EventBatch batch = record.value();
            if (batch == null) continue;

            // 1. Kiem tra tinh Idempotency (chong dem trung)
            boolean isNew = rollupRepo.tryRecordBatch(batch.batchId(), batch.tenantId());
            if (!isNew) {
                batchSkipped++;
                skippedDuplicates.incrementAndGet();
                continue;
            }

            // 2. Phan loai va gom bo dem theo phut cua event
            long bucketMinute = (batch.timestamp() != null)
                    ? batch.timestamp().getEpochSecond() / 60
                    : System.currentTimeMillis() / 60000;

            Map<Outcome, Long> counts = new EnumMap<>(Outcome.class);
            for (EventItem item : batch.items()) {
                Outcome outcome = EventClassifier.classify(item);
                counts.put(outcome, counts.getOrDefault(outcome, 0L) + 1);
            }

            // 3. Cap nhat vao Database
            rollupRepo.recordAggregates(batch.tenantId(), bucketMinute, counts);
            batchApplied++;
            appliedBatches.incrementAndGet();
            totalEventsProcessed.addAndGet(batch.items().size());
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("Processed batch of {} records in {}ms (applied={}, skipped={})",
                records.size(), elapsed, batchApplied, batchSkipped);
    }

    public int getAppliedBatches() {
        return appliedBatches.get();
    }

    public int getSkippedDuplicates() {
        return skippedDuplicates.get();
    }

    public long getTotalEventsProcessed() {
        return totalEventsProcessed.get();
    }

    public void reset() {
        appliedBatches.set(0);
        skippedDuplicates.set(0);
        totalEventsProcessed.set(0);
    }
}
