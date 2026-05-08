package dk.dtu.scout.crossover;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static dk.dtu.scout.crossover.CrossoverTestSupport.stateWithParents;
import static org.junit.jupiter.api.Assertions.*;

class KPointCrossoverTest {

    @Test
    void crossover_withOneCutAlternatesAfterCutPoint() {
        KPointCrossover crossover = new KPointCrossover();

        boolean[] parent1 = {true, true, true, true};
        boolean[] parent2 = {false, false, false, false};

        crossover.configure(Map.of("k", 1));
        crossover.init(stateWithParents(parent1, parent2));

        boolean[] child = crossover.crossover(new FixedIntRandom(2));

        assertArrayEquals(new boolean[] {true, true, true, false}, child);
    }

    @Test
    void crossover_withTwoCutsAlternatesBetweenParents() {
        KPointCrossover crossover = new KPointCrossover();

        boolean[] parent1 = {true, true, true, true, true};
        boolean[] parent2 = {false, false, false, false, false};

        crossover.configure(Map.of("k", 2));
        crossover.init(stateWithParents(parent1, parent2));

        boolean[] child = crossover.crossover(new FixedIntRandom(1, 3));

        assertArrayEquals(new boolean[] {true, true, false, false, true}, child);
    }

    @Test
    void crossover_sortsDistinctRandomCuts() {
        KPointCrossover crossover = new KPointCrossover();

        boolean[] parent1 = {true, true, true, true, true};
        boolean[] parent2 = {false, false, false, false, false};

        crossover.configure(Map.of("k", 2));
        crossover.init(stateWithParents(parent1, parent2));

        boolean[] child = crossover.crossover(new FixedIntRandom(3, 1));

        assertArrayEquals(new boolean[] {true, true, false, false, true}, child);
    }

    @Test
    void crossover_handlesDuplicateRandomCutsBySamplingAgain() {
        KPointCrossover crossover = new KPointCrossover();

        boolean[] parent1 = {true, true, true, true, true};
        boolean[] parent2 = {false, false, false, false, false};

        crossover.configure(Map.of("k", 2));
        crossover.init(stateWithParents(parent1, parent2));

        boolean[] child = crossover.crossover(new FixedIntRandom(1, 1, 3));

        assertArrayEquals(new boolean[] {true, true, false, false, true}, child);
    }

    @Test
    void crossover_withEmptyParentsReturnsEmptyChild() {
        KPointCrossover crossover = new KPointCrossover();

        crossover.init(stateWithParents(new boolean[0], new boolean[0]));

        assertArrayEquals(new boolean[0], crossover.crossover(new Random(1234L)));
    }

    @Test
    void crossover_withLengthOneChoosesOneOfTheParents() {
        KPointCrossover crossover = new KPointCrossover();

        crossover.init(stateWithParents(new boolean[] {true}, new boolean[] {false}));

        assertArrayEquals(new boolean[] {true}, crossover.crossover(new FixedBooleanRandom(true)));
        assertArrayEquals(new boolean[] {false}, crossover.crossover(new FixedBooleanRandom(false)));
    }

    @Test
    void crossover_rejectsTooManyCutPoints() {
        KPointCrossover crossover = new KPointCrossover();

        crossover.configure(Map.of("k", 3));
        crossover.init(stateWithParents(
            new boolean[] {true, true, true},
            new boolean[] {false, false, false}
        ));

        assertThrows(IllegalArgumentException.class, () -> crossover.crossover(new Random(1234L)));
    }

    @Test
    void configure_rejectsNonPositiveK() {
        KPointCrossover crossover = new KPointCrossover();

        assertThrows(IllegalArgumentException.class, () -> crossover.configure(Map.of("k", 0)));
        assertThrows(IllegalArgumentException.class, () -> crossover.configure(Map.of("k", -1)));
    }

    @Test
    void metadata_isStable() {
        KPointCrossover crossover = new KPointCrossover();

        assertEquals("k-point", crossover.id());
        assertEquals("K-Point Crossover", crossover.displayName());
        assertFalse(crossover.description().isBlank());
        assertEquals(1, crossover.params().size());
        assertEquals(List.of("bitstring"), crossover.supportedSearchSpaces());
    }

    private static final class FixedIntRandom extends Random {
        private final int[] values;
        private int index;

        private FixedIntRandom(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            return Math.floorMod(values[index++], bound);
        }
    }

    private static final class FixedBooleanRandom extends Random {
        private final boolean value;

        private FixedBooleanRandom(boolean value) {
            this.value = value;
        }

        @Override
        public boolean nextBoolean() {
            return value;
        }
    }
}