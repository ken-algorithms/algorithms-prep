package com.prep.kafka.repository;

import com.prep.kafka.model.Outcome;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

@Repository
public class RollupRepository {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public RollupRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.tx = jdbc == null ? null : new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource()));
    }

    /**
     * Danh dau batch + cong bo dem trong CUNG MOT transaction: ca hai, hoac khong gi ca.
     *
     * <p>Ban cu goi {@link #tryRecordBatch} roi moi {@link #recordAggregates}, hai transaction rieng: consumer chet o
     * giua thi batch da bi danh dau "da xu ly" nhung so dem chua cong, va lan doc lai se BO QUA no (mat so lieu).
     *
     * @return true neu batch duoc ap dung lan nay, false neu da ap dung truoc do (retry hoac Kafka gui lai)
     */
    public boolean applyBatchOnce(String batchId, String tenantId, long bucketMinute, Map<Outcome, Long> counts) {
        try {
            return Boolean.TRUE.equals(tx.execute(s -> {
                int marked = jdbc.update("INSERT INTO processed_batch (batch_id, tenant_id) "
                        + "SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM processed_batch WHERE batch_id = ?)",
                        batchId, tenantId, batchId);
                if (marked == 0) {
                    return false;
                }
                recordAggregates(tenantId, bucketMinute, counts);
                return true;
            }));
        } catch (DuplicateKeyException race) {
            return false; // hai consumer cung xu ly mot batch luc rebalance: transaction thua da rollback toan bo
        }
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
     * Cong don (cnt = cnt + n): KHONG idempotent mot minh, nen chi duoc goi tu {@link #applyBatchOnce}.
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
