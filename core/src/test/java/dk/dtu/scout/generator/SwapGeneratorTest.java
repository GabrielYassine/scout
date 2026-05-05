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
    void generate_fallsBackToSelectedParentWhenOffspringBaseMissing() {
        SwapGenerator generator = new SwapGenerator();
        int[] parent = new int[] {0, 1, 2, 3, 4};

        State state = new State();
        state.update(Map.of(StateKeys.SELECTED_PARENT_1, parent));
        generator.init(state);

        int[] result = generator.generate(new Random(1234L));

        assertArrayEquals(sorted(parent), sorted(result));
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
    void generate_rejectsMissingBaseAndParent() {
        SwapGenerator generator = new SwapGenerator();

        generator.init(new State());

        assertThrows(IllegalStateException.class, () -> generator.generate(new Random(1234L)));
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