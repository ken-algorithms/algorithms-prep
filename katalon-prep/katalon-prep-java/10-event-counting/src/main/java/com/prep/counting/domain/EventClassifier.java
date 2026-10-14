package com.prep.counting.domain;

/** Decides which of the four counts one item belongs to. Check the key first: a fake key is fake whatever its value. */
public final class EventClassifier {

    private final KeyRegistry registry;

    public EventClassifier(KeyRegistry registry) {
        this.registry = registry;
    }

    public Outcome classify(String key, Object value) {
        if (key == null || !registry.contains(key)) {
            return Outcome.FAKE_KEY;
        }
        if (value instanceof Boolean b) {
            return b ? Outcome.TRUE : Outcome.FALSE;
        }
        if (value instanceof String s) {
            if (s.equalsIgnoreCase("true")) {
                return Outcome.TRUE;
            }
            if (s.equalsIgnoreCase("false")) {
                return Outcome.FALSE;
            }
        }
        return Outcome.MALFORMED; // null, 1, "yes": a client bug we want to see, not hide inside false
    }
}
