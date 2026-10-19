package com.prep.cs.counting;

/** Bon ket qua cua mot cap key-value. Thu tu ordinal() la chi so trong mang dem int[4]. */
public enum Outcome {
    TRUE, FALSE, FAKE_KEY, MALFORMED;

    static final Outcome[] ALL = values();
}
