package com.prep.counting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.prep.counting.domain.EventClassifier;
import com.prep.counting.domain.KeyRegistry;
import com.prep.counting.domain.Outcome;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EventClassifierTest {

    private final EventClassifier c = new EventClassifier(new KeyRegistry(Set.of("checkout_v2", "dark_mode")));

    @Test
    void fourOutcomes() {
        assertEquals(Outcome.TRUE, c.classify("checkout_v2", true));
        assertEquals(Outcome.FALSE, c.classify("dark_mode", false));
        assertEquals(Outcome.FAKE_KEY, c.classify("does_not_exist", true));
        assertEquals(Outcome.MALFORMED, c.classify("dark_mode", "yes"));
    }

    @Test
    void keyIsCheckedBeforeValue() {
        assertEquals(Outcome.FAKE_KEY, c.classify("does_not_exist", "garbage"));
        assertEquals(Outcome.FAKE_KEY, c.classify(null, true));
    }

    @Test
    void stringBooleansAreAcceptedButNotNumbersOrNull() {
        assertEquals(Outcome.TRUE, c.classify("dark_mode", "TRUE"));
        assertEquals(Outcome.FALSE, c.classify("dark_mode", "false"));
        assertEquals(Outcome.MALFORMED, c.classify("dark_mode", 1));
        assertEquals(Outcome.MALFORMED, c.classify("dark_mode", null));
    }
}
