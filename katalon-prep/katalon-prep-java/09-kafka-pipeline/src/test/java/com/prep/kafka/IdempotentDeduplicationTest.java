package com.prep.kafka;

import com.prep.kafka.consumer.BatchEventConsumer;
import com.prep.kafka.model.EventBatch;
import com.prep.kafka.model.EventItem;
import com.prep.kafka.model.Outcome;
import com.prep.kafka.repository.RollupRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IdempotentDeduplicationTest {

    private RollupRepository rollupRepo;
    private BatchEventConsumer consumer;

    @BeforeEach
    void setUp() {
        DataSource ds = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:schema.sql")
                .build();
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        rollupRepo = new RollupRepository(jdbc);
        consumer = new BatchEventConsumer(rollupRepo);
    }

    @Test
    @DisplayName("Idempotency: Gửi lại 40 batch trùng lặp do network retry vẫn ra kết quả chính xác 100%")
    void testDuplicateBatchesDoNotInflateCounts() {
        String tenantId = "tenant_idempotent_test";
        Instant now = Instant.now();
        long bucketMinute = now.getEpochSecond() / 60;

        List<EventBatch> originalBatches = new ArrayList<>();
        List<ConsumerRecord<String, EventBatch>> initialRecords = new ArrayList<>();

        // 1. Tạo 100 batch ban đầu (mỗi batch có 1 true, 1 fake key)
        for (int i = 0; i < 100; i++) {
            String batchId = "batch_" + UUID.randomUUID();
            EventBatch batch = new EventBatch(
                    batchId,
                    now,
                    tenantId,
                    List.of(new EventItem("feature_10", true), new EventItem("non_existent_key", false))
            );
            originalBatches.add(batch);
            initialRecords.add(new ConsumerRecord<>("events", 0, i, batchId, batch));
        }

        // Xử lý đợt 1
        consumer.processBatchRecords(initialRecords);
        assertEquals(100, consumer.getAppliedBatches());
        assertEquals(0, consumer.getSkippedDuplicates());
        assertEquals(100, rollupRepo.getCount(tenantId, bucketMinute, Outcome.TRUE));
        assertEquals(100, rollupRepo.getCount(tenantId, bucketMinute, Outcome.FAKE_KEY));

        // 2. Giả lập Client bị timeout mạng nên RETRY gửi lại 40 batch trong số 100 batch trên
        List<ConsumerRecord<String, EventBatch>> retryRecords = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            EventBatch retryBatch = originalBatches.get(i);
            retryRecords.add(new ConsumerRecord<>("events", 0, 100 + i, retryBatch.batchId(), retryBatch));
        }

        // Xử lý đợt 2 (đợt gửi trùng)
        consumer.processBatchRecords(retryRecords);

        // Kiểm chứng:
        // - Số batch áp dụng vẫn là 100
        // - Số batch trùng bị bỏ qua là 40
        // - Tổng số đếm trong DB không đổi (reconcile_drift = 0)
        assertEquals(100, consumer.getAppliedBatches());
        assertEquals(40, consumer.getSkippedDuplicates());
        assertEquals(100, rollupRepo.getCount(tenantId, bucketMinute, Outcome.TRUE));
        assertEquals(100, rollupRepo.getCount(tenantId, bucketMinute, Outcome.FAKE_KEY));
        assertEquals(100, rollupRepo.getTotalProcessedBatches());

        System.out.printf("=== Idempotency verified: applied=%d, duplicate skipped=%d, reconcile_drift=0!%n",
                consumer.getAppliedBatches(), consumer.getSkippedDuplicates());
    }
}
