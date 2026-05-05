package dk.dtu.scout.problems;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LeadingOnesTest {

    @Test
    void fitness_countsOnlyConsecutiveTruePrefix() {
        LeadingOnes problem = new LeadingOnes();

        assertEquals(0.0, problem.fitness(new boolean[] {false, true, true}));
        assertEquals(2.0, problem.fitness(new boolean[] {true, true, false, true}));
        assertEquals(4.0, problem.fitness(new boolean[] {true, true, true, true}));
    }

    @Test
    void configure_setsDimensionUsedForOptimality() {
        LeadingOnes problem = new LeadingOnes();

        problem.configure(Map.of("n", 4));

        assertFalse(problem.isOptimal(3.0));
        assertTrue(problem.isOptimal(4.0));
    }

    @Test
    void configure_rejectsNonPositiveDimension() {
        LeadingOnes problem = new LeadingOnes();

        assertThrows(IllegalArgumentException.class, () -> problem.configure(Map.of("n", 0)));
        assertThrows(IllegalArgumentException.class, () -> problem.configure(Map.of("n", -1)));
    }

    @Test
    void init_setsDimensionFromSharedState() {
        LeadingOnes problem = new LeadingOnes();
        State state = new State();

        state.update(Map.of(StateKeys.DIMENSION, 3));
        problem.init(state);

        assertFalse(problem.isOptimal(2.0));
        assertTrue(problem.isOptimal(3.0));
    }

    @Test
    void metadata_isStable() {
        LeadingOnes problem = new LeadingOnes();

        assertEquals("leadingones", problem.id());
        assertEquals("LeadingOnes", problem.displayName());
        assertFalse(problem.description().isBlank());
        assertEquals("bitstring", problem.supportedSearchSpaces().getFirst());
        assertTrue(problem.params().isEmpty());
    }
}