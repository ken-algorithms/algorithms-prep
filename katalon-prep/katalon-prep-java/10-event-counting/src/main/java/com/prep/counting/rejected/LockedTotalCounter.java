package com.prep.counting.rejected;

import com.prep.counting.domain.EventBatch;
import com.prep.counting.domain.EventClassifier;
import com.prep.counting.domain.Outcome;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * REJECTED approach 1 — the answer given in the interview: "on insert, lock and add to the totals; to answer,
 * read the last record". Kept only so the tests can show what goes wrong.
 *
 * <ul>
 *   <li>No range query: one running total has no time left in it.</li>
 *   <li>A retried batch is added twice, and nothing can tell.</li>
 *   <li>Every writer waits for the same row lock.</li>
 *   <li>No raw: a wrong number can never be recomputed.</li>
 * </ul>
 */
public final class LockedTotalCounter {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final EventClassifier classifier;

    public LockedTotalCounter(JdbcTemplate jdbc, TransactionTemplate tx, EventClassifier classifier) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.classifier = classifier;
    }

    public void ingest(EventBatch b) {
        long[] c = new long[4];
        for (EventBatch.Item it : b.items()) {
            c[classifier.classify(it.key(), it.value()).code]++;
        }
        jdbc.update("INSERT INTO naive_total (tenant_id) VALUES (?) ON CONFLICT DO NOTHING", b.tenantId());
        tx.executeWithoutResult(s -> {
            // "lock while summarizing": every request for this tenant queues here
            jdbc.queryForList("SELECT 1 FROM naive_total WHERE tenant_id = ? FOR UPDATE", b.tenantId());
            jdbc.update("UPDATE naive_total SET false_cnt = false_cnt + ?, true_cnt = true_cnt + ?, "
                    + "fake_cnt = fake_cnt + ?, malformed_cnt = malformed_cnt + ?, updated_at = now() "
                    + "WHERE tenant_id = ?", c[0], c[1], c[2], c[3], b.tenantId());
        });
    }

    /** "Read the last record": the only question this design can answer is "how many since the beginning". */
    public Map<Outcome, Long> lastRecord(String tenantId) {
        return jdbc.queryForObject("SELECT false_cnt, true_cnt, fake_cnt, malformed_cnt FROM naive_total "
                + "WHERE tenant_id = ?", (rs, i) -> Map.of(Outcome.FALSE, rs.getLong(1), Outcome.TRUE, rs.getLong(2),
                        Outcome.FAKE_KEY, rs.getLong(3), Outcome.MALFORMED, rs.getLong(4)), tenantId);
    }
}
