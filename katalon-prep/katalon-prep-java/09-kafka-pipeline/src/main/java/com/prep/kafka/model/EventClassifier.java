package com.prep.kafka.model;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class EventClassifier {

    // Danh sach key hop le (vi du 500 feature keys)
    private static final Set<String> VALID_KEYS = IntStream.range(0, 500)
            .mapToObj(i -> "feature_" + i)
            .collect(Collectors.toUnmodifiableSet());

    private EventClassifier() {}

    public static Outcome classify(EventItem item) {
        if (item == null || item.key() == null || item.key().isBlank() || item.value() == null) {
            return Outcome.MALFORMED;
        }
        if (!VALID_KEYS.contains(item.key())) {
            return Outcome.FAKE_KEY;
        }
        return item.value() ? Outcome.TRUE : Outcome.FALSE;
    }
}
