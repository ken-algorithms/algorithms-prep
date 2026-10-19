package com.prep.onbai.outbox;

import java.util.random.RandomGenerator;

/** Exponential backoff "full jitter" (AWS Architecture Blog): ngau nhien trong [0, min(cap, base x 2^attempt)]. */
public final class Backoff {

    private Backoff() {}

    public static long fullJitterMs(int attempt, long baseMs, long capMs, RandomGenerator rnd) {
        long ceiling = Math.min(capMs, baseMs << Math.min(attempt, 30));
        return rnd.nextLong(ceiling + 1);
    }
}
