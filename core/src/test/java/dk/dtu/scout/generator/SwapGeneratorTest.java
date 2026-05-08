package dk.dtu.scout.generator;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static dk.dtu.scout.generator.GeneratorTestSupport.sorted;
import static dk.dtu.scout.generator.GeneratorTestSupport.stateWithBase;
import static org.junit.jupiter.api.Assertions.*;

class SwapGeneratorTest {

    @Test
    void generate_swapsTwoPositionsFromOffspringBase() {
        SwapGenerator generator = new SwapGenerator();
        int[] base = new int[] {0, 1, 2, 3, 4};

        generator.init(stateWithBase(base));

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

        generator.init(stateWithBase(base));

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
}