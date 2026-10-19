package com.prep.cs.counting;

/** Bon tang bucket, can theo UTC tu epoch. Bucket = so giay epoch cua dau bucket. */
public enum Tier {
    SECOND(1), MINUTE(60), HOUR(3_600), DAY(86_400);

    public final int seconds;

    Tier(int seconds) {
        this.seconds = seconds;
    }

    public long bucketOf(long epochSecond) {
        return Math.floorDiv(epochSecond, seconds) * seconds;
    }

    static final Tier[] COARSE_FIRST = {DAY, HOUR, MINUTE, SECOND};
}
