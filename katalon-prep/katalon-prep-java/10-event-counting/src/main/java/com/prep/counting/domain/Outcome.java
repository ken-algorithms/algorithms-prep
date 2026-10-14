package com.prep.counting.domain;

/** The four counts the question asks for. Stored as a smallint: 288M rows a day, so one byte matters. */
public enum Outcome {
    FALSE(0),      // valid key, value false
    TRUE(1),       // valid key, value true
    FAKE_KEY(2),   // key not in the registry
    MALFORMED(3);  // valid key, value is not a boolean: counted, never folded into false

    public final short code;

    Outcome(int code) {
        this.code = (short) code;
    }

    public static Outcome of(int code) {
        return values()[code];
    }
}
