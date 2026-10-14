package com.prep.counting.rejected;

import static com.prep.counting.domain.Times.utc;

import com.prep.counting.domain.EventBatch;
import com.prep.counting.domain.EventClassifier;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * REJECTED approach 3 — per-minute counters bumped at insert time ({@code cnt = cnt + n}), no raw table. A Redis
 * HINCRBY per hour is the same design in memory. It does answer any minute-aligned range, so it fixes approach 1's
 * fatal flaw. Rejected because an increment is atomic but not idempotent: a retry counts twice, and with no raw
 * there is nothing to recompute the right number from.
 */
public final class SyncMinuteCounter {

    private final JdbcTemplate jdbc;
    private final EventClassifier classifier;

    public SyncMinuteCounter(JdbcTemplate jdbc, EventClassifier classifier) {
        this.jdbc = jdbc;
        this.classifier = classifier;
    }

    public void ingest(EventBatch b) {
        long[] c = new long[4];
        for (EventBatch.Item it : b.items()) {
            c[classifier.classify(it.key(), it.value()).code]++;
        }
        for (int outcome = 0; outcome < 4; outcome++) {
            if (c[outcome] == 0) {
                continue;
            }
            jdbc.update("INSERT INTO sync_agg_1m (tenant_id, bucket, outcome, cnt) "
                    + "VALUES (?, date_bin('1 minute', ?::timestamptz, TIMESTAMPTZ '2000-01-01 00:00:00+00'), ?, ?) "
                    + "ON CONFLICT (tenant_id, bucket, outcome) DO UPDATE SET cnt = sync_agg_1m.cnt + EXCLUDED.cnt",
                    b.tenantId(), utc(b.ts()), (short) outcome, c[outcome]);
        }
    }

    public long[] count(String tenantId, Instant from, Instant to) {
        long[] c = new long[4];
        jdbc.query("SELECT outcome, sum(cnt) FROM sync_agg_1m WHERE tenant_id = ? AND bucket >= ? AND bucket < ? "
                + "GROUP BY 1", rs -> {
                    c[rs.getInt(1)] = rs.getLong(2);
                }, tenantId, utc(from), utc(to));
        return c;
    }
}
