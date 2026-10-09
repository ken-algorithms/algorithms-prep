package com.prep.kafka.repository;

import com.prep.kafka.model.Outcome;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Repository
public class RollupRepository {

    private final JdbcTemplate jdbc;

    public RollupRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Kiem tra va danh dau batch da duoc xu ly chua (Idempotency Store).
     * @return true neu batch moi (chua tung xu ly), false neu la duplicate.
     */
    public boolean tryRecordBatch(String batchId, String tenantId) {
        try {
            int inserted = jdbc.update(
                    "INSERT INTO processed_batch (batch_id, tenant_id) VALUES (?, ?)",
                    batchId, tenantId
            );
            return inserted == 1;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /**
     * Cap nhat bo dem rollup theo phut.
     * Ho tro ca cong don (counter increment) va GREATEST de chong duyet trung.
     */
    @Transactional
    public void recordAggregates(String tenantId, long bucketMinute, Map<Outcome, Long> counts) {
        for (var entry : counts.entrySet()) {
            if (entry.getValue() <= 0) continue;
            String outcome = entry.getKey().name();
            long delta = entry.getValue();

            int updated = jdbc.update(
                    "UPDATE agg_1m SET cnt = cnt + ? WHERE tenant_id = ? AND bucket_minute = ? AND outcome = ?",
                    delta, tenantId, bucketMinute, outcome
            );
            if (updated == 0) {
                try {
                    jdbc.update(
                            "INSERT INTO agg_1m (tenant_id, bucket_minute, outcome, cnt) VALUES (?, ?, ?, ?)",
                            tenantId, bucketMinute, outcome, delta
                    );
                } catch (DuplicateKeyException e) {
                    jdbc.update(
                            "UPDATE agg_1m SET cnt = cnt + ? WHERE tenant_id = ? AND bucket_minute = ? AND outcome = ?",
                            delta, tenantId, bucketMinute, outcome
                    );
                }
            }
        }
    }

    public long getCount(String tenantId, long bucketMinute, Outcome outcome) {
        var list = jdbc.query(
                "SELECT cnt FROM agg_1m WHERE tenant_id = ? AND bucket_minute = ? AND outcome = ?",
                (rs, rowNum) -> rs.getLong("cnt"),
                tenantId, bucketMinute, outcome.name()
        );
        return list.isEmpty() ? 0L : list.get(0);
    }

    public long getTotalProcessedBatches() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM processed_batch", Long.class);
        return count != null ? count : 0L;
    }
}
