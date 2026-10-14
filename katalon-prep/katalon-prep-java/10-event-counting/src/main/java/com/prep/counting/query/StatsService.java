package com.prep.counting.query;

import static com.prep.counting.domain.Times.utc;

import com.prep.counting.rollup.RollupJob;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/** The read path: decompose the range, sum the pieces. Cost depends on the range shape, never on the write rate. */
public final class StatsService {

    private final JdbcTemplate jdbc;
    private final RollupJob rollup;

    public StatsService(JdbcTemplate jdbc, RollupJob rollup) {
        this.jdbc = jdbc;
        this.rollup = rollup;
    }

    public Stats query(String tenantId, Instant from, Instant to) {
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
        List<RangeDecomposer.Part> parts = RangeDecomposer.decompose(from, to, rollup.sealedUntil());
        long[] counts = new long[4];
        long[] rows = new long[2]; // [0] bucket rows read, [1] raw rows counted
        for (RangeDecomposer.Part p : parts) {
            if (p.tier() == Tier.RAW) {
                jdbc.query("SELECT outcome, count(*) FROM raw_events WHERE tenant_id = ? AND ts >= ? AND ts < ? "
                        + "GROUP BY outcome", rs -> {
                            counts[rs.getInt(1)] += rs.getLong(2);
                            rows[1] += rs.getLong(2);
                        }, tenantId, utc(p.from()), utc(p.to()));
            } else {
                jdbc.query("SELECT outcome, sum(cnt), count(*) FROM " + p.tier().table
                        + " WHERE tenant_id = ? AND bucket >= ? AND bucket < ? GROUP BY outcome", rs -> {
                            counts[rs.getInt(1)] += rs.getLong(2);
                            rows[0] += rs.getLong(3);
                        }, tenantId, utc(p.from()), utc(p.to()));
            }
        }
        return Stats.of(counts, parts.size(), rows[0], rows[1]);
    }
}
