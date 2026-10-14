package com.prep.counting.rollup;

import static com.prep.counting.domain.Times.ceil;
import static com.prep.counting.domain.Times.floor;
import static com.prep.counting.domain.Times.utc;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Turns raw events into the three rollup tiers. Runs every minute.
 *
 * <p>The one rule: a bucket's count is RECOMPUTED from its source and REPLACED, never incremented. Running the
 * job twice, or after a consumer replayed a batch, gives the same numbers. Late events inside the last
 * {@link #RECOMPUTE_WINDOW} land in the right bucket on the next run; older ones are caught by {@link Reconciler}.
 */
public final class RollupJob {

    public static final Duration RECOMPUTE_WINDOW = Duration.ofMinutes(30);
    static final Duration MINUTE = Duration.ofMinutes(1);
    static final Duration HOUR = Duration.ofHours(1);
    static final Duration DAY = Duration.ofDays(1);
    private static final String ORIGIN = "TIMESTAMPTZ '2000-01-01 00:00:00+00'";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final Clock clock;

    public RollupJob(JdbcTemplate jdbc, TransactionTemplate tx, Clock clock) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.clock = clock;
    }

    /** The scheduled call: recompute the last 30 minutes, then seal up to the start of the running minute. */
    public Instant refreshRecent() {
        Instant to = floor(clock.instant(), MINUTE); // the running minute is still filling: never seal it
        Instant from = to.minus(RECOMPUTE_WINDOW);
        tx.executeWithoutResult(s -> {
            replaceAll(from, to);
            jdbc.update("UPDATE rollup_state SET sealed_until = GREATEST(sealed_until, ?) WHERE id = 1", utc(to));
        });
        return to;
    }

    /** Backfill or repair any closed window, e.g. after a late batch or a reconcile drift. Does not move the seal. */
    public void refresh(Instant from, Instant to) {
        tx.executeWithoutResult(s -> replaceAll(floor(from, MINUTE), ceil(to, MINUTE)));
    }

    private void replaceAll(Instant from, Instant to) {
        // minutes from raw, hours from minutes, days from hours: each tier reads the one below it
        replace("agg_1m", "1 minute", "raw_events", "ts", "count(*)", from, to);
        replace("agg_1h", "1 hour", "agg_1m", "bucket", "sum(cnt)",
                floor(from, HOUR), ceil(to, HOUR));
        replace("agg_1d", "1 day", "agg_1h", "bucket", "sum(cnt)",
                floor(from, DAY), ceil(to, DAY));
    }

    private void replace(String target, String width, String source, String timeCol, String count,
                         Instant from, Instant to) {
        OffsetDateTime f = utc(from);
        OffsetDateTime t = utc(to);
        // DELETE + INSERT in one transaction = overwrite. Readers see the old rows until commit, then the new ones.
        jdbc.update("DELETE FROM " + target + " WHERE bucket >= ? AND bucket < ?", f, t);
        jdbc.update("INSERT INTO " + target + " (tenant_id, bucket, outcome, cnt) "
                + "SELECT tenant_id, date_bin('" + width + "', " + timeCol + ", " + ORIGIN + "), outcome, " + count
                + " FROM " + source + " WHERE " + timeCol + " >= ? AND " + timeCol + " < ? GROUP BY 1, 2, 3", f, t);
    }

    public Instant sealedUntil() {
        return jdbc.queryForObject("SELECT sealed_until FROM rollup_state WHERE id = 1", OffsetDateTime.class).toInstant();
    }
}
