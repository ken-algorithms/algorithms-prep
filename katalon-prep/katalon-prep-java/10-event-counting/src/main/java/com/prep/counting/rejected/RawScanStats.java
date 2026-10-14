package com.prep.counting.rejected;

import static com.prep.counting.domain.Times.utc;

import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * REJECTED approach 2 as a read path — store every event, COUNT(*) when asked. Always correct, which is why the
 * tests use it as the oracle. Rejected because its cost grows with the data: "last 24 hours" at 10k requests a
 * minute is 288 million rows to count on every dashboard refresh.
 */
public final class RawScanStats {

    private final JdbcTemplate jdbc;

    public RawScanStats(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Counts per outcome code, index 0..3. */
    public long[] count(String tenantId, Instant from, Instant to) {
        long[] c = new long[4];
        jdbc.query("SELECT outcome, count(*) FROM raw_events WHERE tenant_id = ? AND ts >= ? AND ts < ? GROUP BY 1",
                rs -> {
                    c[rs.getInt(1)] = rs.getLong(2);
                }, tenantId, utc(from), utc(to));
        return c;
    }
}
