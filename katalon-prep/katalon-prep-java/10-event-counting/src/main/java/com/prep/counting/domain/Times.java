package com.prep.counting.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Bucket arithmetic in UTC, on the same origin as SQL's {@code date_bin(width, ts, '2000-01-01+00')}. */
public final class Times {

    private Times() {}

    public static Instant floor(Instant t, Duration step) {
        long s = step.getSeconds();
        return Instant.ofEpochSecond(Math.floorDiv(t.getEpochSecond(), s) * s);
    }

    public static Instant ceil(Instant t, Duration step) {
        Instant f = floor(t, step);
        return f.equals(t) ? t : f.plus(step);
    }

    public static boolean aligned(Instant t, Duration step) {
        return t.getNano() == 0 && floor(t, step).equals(t);
    }

    /** JDBC parameter for a timestamptz column: never java.sql.Timestamp, which drags in the JVM time zone. */
    public static OffsetDateTime utc(Instant t) {
        return t.atOffset(ZoneOffset.UTC);
    }
}
