package com.prep.counting.query;

import java.time.Duration;

/** Where a piece of a time range is read from, coarsest first. */
public enum Tier {
    DAY("agg_1d", Duration.ofDays(1)),
    HOUR("agg_1h", Duration.ofHours(1)),
    MINUTE("agg_1m", Duration.ofMinutes(1)),
    RAW("raw_events", null);

    public final String table;
    public final Duration step;

    Tier(String table, Duration step) {
        this.table = table;
        this.step = step;
    }
}
