package dk.dtu.scout.stopcondition;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.problems.Problem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OptimumReachedTest {

    @Test
    void shouldStop_isFalseWhenProblemIsMissing() {
        OptimumReached<Object> stopCondition = new OptimumReached<>();

        assertFalse(stopCondition.shouldStop(0, 0, 10.0, null));
    }

    @Test
    void shouldStop_usesProblemSetDirectly() {
        OptimumReached<Object> stopCondition = new OptimumReached<>();

        stopCondition.setProblem(new ThresholdProblem(5.0));

        assertFalse(stopCondition.shouldStop(0, 0, 4.0, null));
        assertTrue(stopCondition.shouldStop(0, 0, 5.0, null));
    }

    @Test
    void init_readsProblemFromSharedState() {
        OptimumReached<Object> stopCondition = new OptimumReached<>();
        State state = new State();

        state.update(Map.of(StateKeys.PROBLEM, new ThresholdProblem(3.0)));
        stopCondition.init(state);

        assertFalse(stopCondition.shouldStop(0, 0, 2.0, null));
        assertTrue(stopCondition.shouldStop(0, 0, 3.0, null));
    }

    @Test
    void init_ignoresNullState() {
        OptimumReached<Object> stopCondition = new OptimumReached<>();

        stopCondition.init(null);

        assertFalse(stopCondition.shouldStop(0, 0, 10.0, null));
    }

    @Test
    void init_ignoresNonProblemStateValue() {
        OptimumReached<Object> stopCondition = new OptimumReached<>();
        State state = new State();

        state.update(Map.of(StateKeys.PROBLEM, "not-a-problem"));
        stopCondition.init(state);

        assertFalse(stopCondition.shouldStop(0, 0, 10.0, null));
    }

    @Test
    void metadata_isStable() {
        OptimumReached<Object> stopCondition = new OptimumReached<>();

        assertEquals("optimum-reached", stopCondition.id());
        assertEquals("Optimum Reached", stopCondition.displayName());
        assertFalse(stopCondition.description().isBlank());
        assertTrue(stopCondition.params().isEmpty());
        assertEquals(List.of("bitstring", "permutation", "route-list"), stopCondition.supportedSearchSpaces());
    }

    private record ThresholdProblem(double optimum) implements Problem<Object> {
        @Override
        public double fitness(Object solution) {
            return 0.0;
        }

        @Override
        public boolean isOptimal(double fitness) {
            return fitness >= optimum;
        }

        @Override
        public String id() {
            return "threshold";
        }

        @Override
        public String displayName() {
            return "Threshold";
        }

        @Override
        public String description() {
            return "Threshold problem";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }
}