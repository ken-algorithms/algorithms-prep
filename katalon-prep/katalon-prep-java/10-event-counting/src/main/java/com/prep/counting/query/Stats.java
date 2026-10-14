package com.prep.counting.query;

/**
 * The answer to "how many true, false and fake keys between from and to". The success rate is computed here, at
 * read time, from the two counts: rates are never stored, because a rate cannot be added across buckets.
 */
public record Stats(long trueCount, long falseCount, long fakeKey, long malformed, Double successRate,
                    int parts, long bucketRowsRead, long rawRowsRead) {

    static Stats of(long[] byOutcome, int parts, long bucketRows, long rawRows) {
        long f = byOutcome[0];
        long t = byOutcome[1];
        Double rate = t + f == 0 ? null : (double) t / (t + f);
        return new Stats(t, f, byOutcome[2], byOutcome[3], rate, parts, bucketRows, rawRows);
    }
}
