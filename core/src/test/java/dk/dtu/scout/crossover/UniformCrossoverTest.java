package dk.dtu.scout.crossover;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class UniformCrossoverTest {

    @Test
    void crossover_takesEachBitFromOneOfTheTwoParents() {
        UniformCrossover crossover = new UniformCrossover();

        boolean[] parent1 = {true, true, true, true};
        boolean[] parent2 = {false, false, false, false};

        crossover.init(stateWithParents(parent1, parent2));

        boolean[] child = crossover.crossover(new FixedBooleanRandom(true, false, true, false));

        assertArrayEquals(new boolean[] {true, false, true, false}, child);
    }

    @Test
    void crossover_returnsEmptyChildForEmptyParents() {
        UniformCrossover crossover = new UniformCrossover();

        crossover.init(stateWithParents(new boolean[0], new boolean[0]));

        boolean[] child = crossover.crossover(new Random(1234L));

        assertArrayEquals(new boolean[0], child);
    }

    @Test
    void configure_ignoresParams() {
        UniformCrossover crossover = new UniformCrossover();

        assertDoesNotThrow(() -> crossover.configure(Map.of("ignored", 123)));
        assertDoesNotThrow(() -> crossover.configure(null));
    }

    @Test
    void metadata_isStable() {
        UniformCrossover crossover = new UniformCrossover();

        assertEquals("uniform", crossover.id());
        assertEquals("Uniform Crossover", crossover.displayName());
        assertFalse(crossover.description().isBlank());
        assertTrue(crossover.params().isEmpty());
        assertEquals(List.of("bitstring"), crossover.supportedSearchSpaces());
    }

    private static State stateWithParents(boolean[] parent1, boolean[] parent2) {
        State state = new State();
        state.update(Map.of(
            StateKeys.SELECTED_PARENT_1, parent1,
            StateKeys.SELECTED_PARENT_2, parent2
        ));
        return state;
    }

    private static final class FixedBooleanRandom extends Random {
        private final boolean[] values;
        private int index;

        private FixedBooleanRandom(boolean... values) {
            this.values = values;
        }

        @Override
        public boolean nextBoolean() {
            return values[index++ % values.length];
        }
    }
}