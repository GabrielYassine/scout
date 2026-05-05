package dk.dtu.scout.generator;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SwapGeneratorTest {

    @Test
    void generate_swapsTwoPositionsFromOffspringBase() {
        SwapGenerator generator = new SwapGenerator();
        int[] base = new int[] {0, 1, 2, 3, 4};

        State state = new State();
        state.update(Map.of(StateKeys.OFFSPRING_BASE, base));
        generator.init(state);

        int[] result = generator.generate(new Random(1234L));

        assertEquals(5, result.length);
        assertArrayEquals(sorted(base), sorted(result));
        assertNotSame(base, result);
        assertFalse(Arrays.equals(base, result));
    }

    @Test
    void generate_returnsCloneWhenLengthIsLessThanTwo() {
        SwapGenerator generator = new SwapGenerator();
        int[] base = new int[] {42};

        State state = new State();
        state.update(Map.of(StateKeys.OFFSPRING_BASE, base));
        generator.init(state);

        int[] result = generator.generate(new Random(1234L));

        assertArrayEquals(base, result);
        assertNotSame(base, result);
    }

    @Test
    void metadata_isStable() {
        SwapGenerator generator = new SwapGenerator();

        assertEquals("swap", generator.id());
        assertEquals("Swap Mutation", generator.displayName());
        assertFalse(generator.description().isBlank());
        assertEquals(List.of("permutation"), generator.supportedSearchSpaces());
        assertTrue(generator.params().isEmpty());
    }

    private static int[] sorted(int[] values) {
        int[] copy = values.clone();
        Arrays.sort(copy);
        return copy;
    }
}