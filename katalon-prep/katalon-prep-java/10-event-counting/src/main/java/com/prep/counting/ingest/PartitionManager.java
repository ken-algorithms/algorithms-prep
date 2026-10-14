package com.prep.counting.ingest;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.jdbc.core.JdbcTemplate;

/** One raw partition per UTC day. Retention drops whole partitions instead of DELETE-ing billions of rows. */
public final class PartitionManager {

    private static final DateTimeFormatter SUFFIX = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final JdbcTemplate jdbc;
    private final Set<LocalDate> known = ConcurrentHashMap.newKeySet();

    public PartitionManager(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void ensureFor(Instant ts) {
        ensureDay(LocalDate.ofInstant(ts, ZoneOffset.UTC));
    }

    public synchronized void ensureDay(LocalDate day) {
        if (known.contains(day)) {
            return;
        }
        // bounds carry +00 explicitly: a bare '2026-08-26' would be read in the session's time zone
        jdbc.execute("CREATE TABLE IF NOT EXISTS raw_events_" + day.format(SUFFIX)
                + " PARTITION OF raw_events FOR VALUES FROM ('" + day + " 00:00:00+00') TO ('"
                + day.plusDays(1) + " 00:00:00+00')");
        known.add(day);
    }

    /** Drops every day partition that ends on or before {@code firstKeptDay}. Returns the dropped table names. */
    public synchronized List<String> dropBefore(LocalDate firstKeptDay) {
        List<String> dropped = new ArrayList<>();
        for (String table : partitions()) {
            LocalDate day = LocalDate.parse(table.substring("raw_events_".length()), SUFFIX);
            if (day.isBefore(firstKeptDay)) {
                jdbc.execute("DROP TABLE " + table);
                known.remove(day);
                dropped.add(table);
            }
        }
        return dropped;
    }

    public List<String> partitions() {
        return jdbc.queryForList("""
                SELECT c.relname FROM pg_inherits i
                JOIN pg_class c ON c.oid = i.inhrelid
                JOIN pg_class p ON p.oid = i.inhparent
                WHERE p.relname = 'raw_events' ORDER BY c.relname""", String.class);
    }
}
