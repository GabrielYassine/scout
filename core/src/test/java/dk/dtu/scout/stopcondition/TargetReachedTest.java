package dk.dtu.scout.stopcondition;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.problems.TSP;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TargetReachedTest {

    @Test
    void shouldStop_usesFitnessTargetForNormalProblems() {
        TargetReached<Object> stopCondition = new TargetReached<>();

        stopCondition.configure(Map.of("targetFitness", 5.0));

        assertFalse(stopCondition.shouldStop(0, 0, 4.999, null));
        assertTrue(stopCondition.shouldStop(0, 0, 5.0, null));
        assertTrue(stopCondition.shouldStop(0, 0, 6.0, null));
    }

    @Test
    void shouldStop_usesDefaultTargetZeroWhenNotConfigured() {
        TargetReached<Object> stopCondition = new TargetReached<>();

        stopCondition.configure(Map.of());

        assertFalse(stopCondition.shouldStop(0, 0, -1.0, null));
        assertTrue(stopCondition.shouldStop(0, 0, 0.0, null));
    }

    @Test
    void shouldStop_handlesTspDistanceTargetWhenTargetIsPositive() {
        TargetReached<int[]> stopCondition = new TargetReached<>();
        State state = new State();

        state.update(Map.of(StateKeys.PROBLEM, new TSP()));
        stopCondition.init(state);
        stopCondition.configure(Map.of("targetFitness", 100.0));

        assertFalse(stopCondition.shouldStop(0, 0, -101.0, null));
        assertTrue(stopCondition.shouldStop(0, 0, -100.0, null));
        assertTrue(stopCondition.shouldStop(0, 0, -99.0, null));
    }

    @Test
    void shouldStop_handlesTspDistanceTargetWhenFlagIsTrue() {
        TargetReached<int[]> stopCondition = new TargetReached<>();
        State state = new State();

        state.update(Map.of(StateKeys.PROBLEM, new TSP()));
        stopCondition.init(state);
        stopCondition.configure(Map.of("targetFitness", 100.0, "targetIsDistance", true));

        assertFalse(stopCondition.shouldStop(0, 0, -101.0, null));
        assertTrue(stopCondition.shouldStop(0, 0, -100.0, null));
    }

    @Test
    void shouldStop_usesRawFitnessTargetForTspWhenTargetIsNegativeAndDistanceFlagIsFalse() {
        TargetReached<int[]> stopCondition = new TargetReached<>();
        State state = new State();

        state.update(Map.of(StateKeys.PROBLEM, new TSP()));
        stopCondition.init(state);
        stopCondition.configure(Map.of("targetFitness", -100.0));

        assertFalse(stopCondition.shouldStop(0, 0, -101.0, null));
        assertTrue(stopCondition.shouldStop(0, 0, -100.0, null));
        assertTrue(stopCondition.shouldStop(0, 0, -99.0, null));
    }

    @Test
    void metadata_isStable() {
        TargetReached<Object> stopCondition = new TargetReached<>();

        assertEquals("target-reached", stopCondition.id());
        assertEquals("Target Fitness Reached", stopCondition.displayName());
        assertFalse(stopCondition.description().isBlank());
        assertEquals(2, stopCondition.params().size());
    }
}