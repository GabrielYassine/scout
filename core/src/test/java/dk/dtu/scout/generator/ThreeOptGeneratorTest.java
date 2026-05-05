package dk.dtu.scout.generator;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ThreeOptGeneratorTest {

    @Test
    void generate_preservesPermutationValidity() {
        ThreeOptGenerator generator = new ThreeOptGenerator();
        int[] base = {0, 1, 2, 3, 4, 5};

        State state = stateWithBase(base);
        generator.init(state);

        int[] result = generator.generate(new Random(1234L));

        assertEquals(base.length, result.length);
        assertArrayEquals(sorted(base), sorted(result));
        assertNotSame(base, result);
    }

    @Test
    void generate_fallsBackToSelectedParentWhenOffspringBaseMissing() {
        ThreeOptGenerator generator = new ThreeOptGenerator();
        int[] parent = {0, 1, 2, 3, 4, 5};

        State state = new State();
        state.update(Map.of(StateKeys.SELECTED_PARENT_1, parent));
        generator.init(state);

        int[] result = generator.generate(new Random(1234L));

        assertArrayEquals(sorted(parent), sorted(result));
    }

    @Test
    void generate_returnsCloneForSmallPermutation() {
        ThreeOptGenerator generator = new ThreeOptGenerator();
        int[] base = {0, 1, 2};

        State state = stateWithBase(base);
        generator.init(state);

        int[] result = generator.generate(new Random(1234L));

        assertArrayEquals(base, result);
        assertNotSame(base, result);
    }

    @Test
    void generate_rejectsMissingBaseAndParent() {
        ThreeOptGenerator generator = new ThreeOptGenerator();

        generator.init(new State());

        assertThrows(IllegalStateException.class, () -> generator.generate(new Random(1234L)));
    }

    @Test
    void generate_coversAllSevenMoves() {
        int[] base = {0, 1, 2, 3, 4, 5};

        int[][] expected = {
            {0, 2, 1, 3, 4, 5},
            {0, 1, 2, 4, 3, 5},
            {0, 2, 1, 4, 3, 5},
            {0, 3, 4, 1, 2, 5},
            {0, 4, 3, 1, 2, 5},
            {0, 3, 4, 2, 1, 5},
            {0, 4, 3, 2, 1, 5}
        };

        for (int move = 1; move <= 7; move++) {
            ThreeOptGenerator generator = new ThreeOptGenerator();
            generator.init(stateWithBase(base));

            int[] result = generator.generate(new FixedIntRandom(1, 1, 1, move - 1));

            assertArrayEquals(expected[move - 1], result);
        }
    }

    @Test
    void metadata_isStable() {
        ThreeOptGenerator generator = new ThreeOptGenerator();

        assertEquals("3opt", generator.id());
        assertEquals("3-Opt Mutation", generator.displayName());
        assertFalse(generator.description().isBlank());
        assertTrue(generator.params().isEmpty());
        assertEquals(List.of("permutation"), generator.supportedSearchSpaces());
    }

    private static State stateWithBase(int[] base) {
        State state = new State();
        state.update(Map.of(StateKeys.OFFSPRING_BASE, base));
        return state;
    }

    private static int[] sorted(int[] values) {
        int[] copy = values.clone();
        Arrays.sort(copy);
        return copy;
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
}