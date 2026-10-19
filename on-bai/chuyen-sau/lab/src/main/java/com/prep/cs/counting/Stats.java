package com.prep.cs.counting;

/** Ket qua tra cho dashboard. successRate = TRUE / (TRUE + FALSE): key gia va item hong khong tinh vao. */
public record Stats(long trueCount, long falseCount, long fakeKey, long malformed, long bucketsRead) {

    public double successRate() {
        long n = trueCount + falseCount;
        return n == 0 ? 0 : (double) trueCount / n;
    }

    public long total() {
        return trueCount + falseCount + fakeKey + malformed;
    }

    public boolean sameCounts(long[] c) {
        return trueCount == c[0] && falseCount == c[1] && fakeKey == c[2] && malformed == c[3];
    }

    @Override
    public String toString() {
        return String.format("TRUE=%d FALSE=%d FAKE_KEY=%d MALFORMED=%d successRate=%.4f (doc %d bucket)",
                trueCount, falseCount, fakeKey, malformed, successRate(), bucketsRead);
    }
}
