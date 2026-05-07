package dk.dtu.scout.stopcondition;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MaxEvaluationsTest {

    @Test
    void shouldStop_isFalseBeforeConfiguredLimit() {
        MaxEvaluations<Object> stopCondition = new MaxEvaluations<>();

        stopCondition.configure(Map.of("maxEvaluations", 5));

        assertFalse(stopCondition.shouldStop(0, 0, 0.0, null));
        assertFalse(stopCondition.shouldStop(0, 4, 0.0, null));
    }

    @Test
    void shouldStop_isTrueAtAndAfterConfiguredLimit() {
        MaxEvaluations<Object> stopCondition = new MaxEvaluations<>();

        stopCondition.configure(Map.of("maxEvaluations", 5));

        assertTrue(stopCondition.shouldStop(0, 5, 0.0, null));
        assertTrue(stopCondition.shouldStop(0, 6, 0.0, null));
    }

    @Test
    void configure_ignoresMissingMaxEvaluationsKey() {
        MaxEvaluations<Object> stopCondition = new MaxEvaluations<>();

        stopCondition.configure(Map.of());

        assertFalse(stopCondition.shouldStop(0, 9_999, 0.0, null));
        assertTrue(stopCondition.shouldStop(0, 10_000, 0.0, null));
    }

    @Test
    void metadata_isStable() {
        MaxEvaluations<Object> stopCondition = new MaxEvaluations<>();

        assertEquals("max-evaluations", stopCondition.id());
        assertEquals("Max Evaluations", stopCondition.displayName());
        assertFalse(stopCondition.description().isBlank());
        assertEquals(1, stopCondition.params().size());
    }
}