package dk.dtu.scout.generator;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;


import static dk.dtu.scout.generator.GeneratorTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class TwoOptGeneratorTest {

    @Test
    void generate_reversesSegmentFromOffspringBaseAndKeepsPermutation() {
        TwoOptGenerator generator = new TwoOptGenerator();
        int[] base = new int[] {0, 1, 2, 3, 4};

        generator.init(stateWithBase(base));

        int[] result = generator.generate(new Random(1234L));

        assertEquals(5, result.length);
        assertArrayEquals(sorted(base), sorted(result));
        assertNotSame(base, result);
    }

    @Test
    void generate_fallsBackToSelectedParentWhenOffspringBaseMissing() {
        TwoOptGenerator generator = new TwoOptGenerator();
        int[] parent = new int[] {0, 1, 2, 3, 4};

        generator.init(stateWithSelectedParent(parent));

        int[] result = generator.generate(new Random(1234L));

        assertArrayEquals(sorted(parent), sorted(result));
    }

    @Test
    void generate_returnsCloneWhenLengthIsLessThanTwo() {
        TwoOptGenerator generator = new TwoOptGenerator();
        int[] base = new int[] {42};

        generator.init(stateWithBase(base));

        int[] result = generator.generate(new Random(1234L));

        assertArrayEquals(base, result);
        assertNotSame(base, result);
    }

    @Test
    void generate_handlesEqualRandomPositionsByAdjustingSecondPosition() {
        TwoOptGenerator generator = new TwoOptGenerator();
        int[] base = new int[] {0, 1, 2};

        generator.init(stateWithBase(base));

        int[] result = generator.generate(new FixedIntRandom(1, 1));

        assertArrayEquals(new int[] {0, 2, 1}, result);
    }

    @Test
    void generate_adjustsEqualRandomPositionsWithWraparound() {
        TwoOptGenerator generator = new TwoOptGenerator();
        int[] base = new int[] {0, 1, 2};

        generator.init(stateWithBase(base));

        int[] result = generator.generate(new FixedIntRandom(2, 2));

        assertArrayEquals(new int[] {2, 1, 0}, result);
    }

    @Test
    void generate_swapsRandomPositionsWhenFirstPositionIsLarger() {
        TwoOptGenerator generator = new TwoOptGenerator();
        int[] base = new int[] {0, 1, 2, 3, 4};

        generator.init(stateWithBase(base));

        int[] result = generator.generate(new FixedIntRandom(3, 1));

        assertArrayEquals(new int[] {0, 3, 2, 1, 4}, result);
    }

    @Test
    void generate_rejectsMissingBaseAndParent() {
        TwoOptGenerator generator = new TwoOptGenerator();

        generator.init(new dk.dtu.scout.State());

        assertThrows(IllegalStateException.class, () -> generator.generate(new Random(1234L)));
    }

    @Test
    void metadata_isStable() {
        TwoOptGenerator generator = new TwoOptGenerator();

        assertEquals("2opt", generator.id());
        assertEquals("2-Opt Mutation", generator.displayName());
        assertFalse(generator.description().isBlank());
        assertEquals(List.of("permutation"), generator.supportedSearchSpaces());
        assertTrue(generator.params().isEmpty());
    }
}