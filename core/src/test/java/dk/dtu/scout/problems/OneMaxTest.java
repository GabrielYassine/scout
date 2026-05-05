package dk.dtu.scout.problems;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OneMaxTest {

    @Test
    void fitness_countsTrueBits() {
        OneMax problem = new OneMax();

        assertEquals(0.0, problem.fitness(new boolean[] {false, false, false}));
        assertEquals(2.0, problem.fitness(new boolean[] {true, false, true, false}));
        assertEquals(4.0, problem.fitness(new boolean[] {true, true, true, true}));
    }

    @Test
    void configure_setsDimensionUsedForOptimality() {
        OneMax problem = new OneMax();

        problem.configure(Map.of("n", 4));

        assertFalse(problem.isOptimal(3.0));
        assertTrue(problem.isOptimal(4.0));
    }

    @Test
    void configure_rejectsNonPositiveDimension() {
        OneMax problem = new OneMax();

        assertThrows(IllegalArgumentException.class, () -> problem.configure(Map.of("n", 0)));
        assertThrows(IllegalArgumentException.class, () -> problem.configure(Map.of("n", -1)));
    }

    @Test
    void init_setsDimensionFromSharedState() {
        OneMax problem = new OneMax();
        State state = new State();

        state.update(Map.of(StateKeys.DIMENSION, 3));
        problem.init(state);

        assertFalse(problem.isOptimal(2.0));
        assertTrue(problem.isOptimal(3.0));
    }

    @Test
    void metadata_isStable() {
        OneMax problem = new OneMax();

        assertEquals("onemax", problem.id());
        assertEquals("OneMax", problem.displayName());
        assertFalse(problem.description().isBlank());
        assertEquals("bitstring", problem.supportedSearchSpaces().getFirst());
        assertTrue(problem.params().isEmpty());
    }
}