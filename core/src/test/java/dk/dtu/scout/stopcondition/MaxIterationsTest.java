package dk.dtu.scout.stopcondition;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MaxIterationsTest {

    @Test
    void shouldStop_isFalseBeforeConfiguredLimit() {
        MaxIterations<Object> stopCondition = new MaxIterations<>();

        stopCondition.configure(Map.of("maxIterations", 5));

        assertFalse(stopCondition.shouldStop(0, 0, 0.0, null));
        assertFalse(stopCondition.shouldStop(4, 0, 0.0, null));
    }

    @Test
    void shouldStop_isTrueAtAndAfterConfiguredLimit() {
        MaxIterations<Object> stopCondition = new MaxIterations<>();

        stopCondition.configure(Map.of("maxIterations", 5));

        assertTrue(stopCondition.shouldStop(5, 0, 0.0, null));
        assertTrue(stopCondition.shouldStop(6, 0, 0.0, null));
    }

    @Test
    void configure_ignoresNullParamsAndUsesDefault() {
        MaxIterations<Object> stopCondition = new MaxIterations<>();

        stopCondition.configure(Map.of());

        assertFalse(stopCondition.shouldStop(9_999, 0, 0.0, null));
        assertTrue(stopCondition.shouldStop(10_000, 0, 0.0, null));
    }

    @Test
    void configure_ignoresMissingMaxIterationsKey() {
        MaxIterations<Object> stopCondition = new MaxIterations<>();

        stopCondition.configure(Map.of());

        assertFalse(stopCondition.shouldStop(9_999, 0, 0.0, null));
        assertTrue(stopCondition.shouldStop(10_000, 0, 0.0, null));
    }

    @Test
    void metadata_isStable() {
        MaxIterations<Object> stopCondition = new MaxIterations<>();

        assertEquals("max-iterations", stopCondition.id());
        assertEquals("Max Iterations", stopCondition.displayName());
        assertFalse(stopCondition.description().isBlank());
        assertEquals(1, stopCondition.params().size());
    }
}