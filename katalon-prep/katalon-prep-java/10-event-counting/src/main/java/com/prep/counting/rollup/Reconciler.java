package com.prep.counting.rollup;

import static com.prep.counting.domain.Times.floor;
import static com.prep.counting.domain.Times.utc;

import com.prep.counting.domain.Outcome;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Proves the numbers instead of trusting them: per hour, COUNT(*) on raw must equal SUM(cnt) on the minute tier.
 * A drift means a lost or double-counted event, or a batch later than the 30-minute window: alert, then repair.
 */
public final class Reconciler {

    public record Drift(Instant hour, String tenantId, Outcome outcome, long raw, long rolledUp) {
        public long diff() {
            return raw - rolledUp;
        }
    }

    public record Report(int hoursChecked, List<Drift> found, List<Drift> afterRepair) {}

    private final JdbcTemplate jdbc;
    private final RollupJob rollup;

    public Reconciler(JdbcTemplate jdbc, RollupJob rollup) {
        this.jdbc = jdbc;
        this.rollup = rollup;
    }

    /** The hourly job: check each closed hour of the last {@code hours} hours, repair the ones that drift. */
    public Report reconcileLastHours(int hours) {
        Instant end = floor(rollup.sealedUntil(), Duration.ofHours(1));
        List<Drift> found = new ArrayList<>();
        List<Drift> after = new ArrayList<>();
        for (int i = hours; i >= 1; i--) {
            Instant from = end.minus(Duration.ofHours(i));
            Instant to = from.plus(Duration.ofHours(1));
            List<Drift> d = check(from, to);
            if (!d.isEmpty()) {
                found.addAll(d);
                rollup.refresh(from, to);
                after.addAll(check(from, to));
            }
        }
        return new Report(hours, found, after);
    }

    public List<Drift> check(Instant from, Instant to) {
        return jdbc.query("""
                WITH r AS (SELECT tenant_id, outcome, count(*) AS n FROM raw_events
                           WHERE ts >= ? AND ts < ? GROUP BY 1, 2),
                     a AS (SELECT tenant_id, outcome, sum(cnt) AS n FROM agg_1m
                           WHERE bucket >= ? AND bucket < ? GROUP BY 1, 2)
                SELECT COALESCE(r.tenant_id, a.tenant_id) AS tenant_id, COALESCE(r.outcome, a.outcome) AS outcome,
                       COALESCE(r.n, 0) AS raw_n, COALESCE(a.n, 0) AS agg_n
                FROM r FULL OUTER JOIN a ON a.tenant_id = r.tenant_id AND a.outcome = r.outcome
                WHERE COALESCE(r.n, 0) <> COALESCE(a.n, 0)""",
                (rs, i) -> new Drift(from, rs.getString("tenant_id"), Outcome.of(rs.getInt("outcome")),
                        rs.getLong("raw_n"), rs.getLong("agg_n")),
                utc(from), utc(to), utc(from), utc(to));
    }
}
