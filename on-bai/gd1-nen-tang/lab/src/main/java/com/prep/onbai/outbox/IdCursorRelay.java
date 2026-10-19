package com.prep.onbai.outbox;

import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * ANTI-PATTERN: relay theo con tro "id > lan truoc". id IDENTITY cap luc INSERT, nhung dong chi HIEN RA luc COMMIT.
 * Transaction lay id 1 commit SAU transaction lay id 2 -> relay da dua con tro qua 2 -> id 1 bi bo sot vinh vien.
 */
public final class IdCursorRelay {

    private final JdbcTemplate jdbc;
    private final FakeBroker broker;
    private long cursor;

    public IdCursorRelay(DataSource ds, FakeBroker broker) {
        this.jdbc = new JdbcTemplate(ds);
        this.broker = broker;
    }

    public int pollOnce(int batchSize) {
        List<long[]> rows = jdbc.query("SELECT id FROM outbox WHERE id > ? ORDER BY id LIMIT ?",
                (rs, i) -> new long[] {rs.getLong(1)}, cursor, batchSize);
        for (long[] r : rows) {
            broker.publish("orders", "?", "id-" + r[0], "{}");
            cursor = r[0];
        }
        return rows.size();
    }
}
