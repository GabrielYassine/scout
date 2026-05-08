package dk.dtu.scout.generator;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static dk.dtu.scout.generator.GeneratorTestSupport.stateWithBase;
import static org.junit.jupiter.api.Assertions.*;

class SingleBitFlipGeneratorTest {

    @Test
    void generate_flipsExactlyOneBitFromOffspringBase() {
        SingleBitFlipGenerator generator = new SingleBitFlipGenerator();
        boolean[] base = new boolean[] {false, false, false, false};

        generator.init(stateWithBase(base));

        boolean[] result = generator.generate(new Random(1234L));

        assertEquals(1, countDifferences(base, result));
        assertNotSame(base, result);
    }

    @Test
    void generate_withEmptyBitstringReturnsSameArray() {
        SingleBitFlipGenerator generator = new SingleBitFlipGenerator();
        boolean[] base = new boolean[0];

        generator.init(stateWithBase(base));

        boolean[] result = generator.generate(new Random(1234L));

        assertSame(base, result);
    }

    @Test
    void generate_rejectsMissingOffspringBase() {
        SingleBitFlipGenerator generator = new SingleBitFlipGenerator();

        State state = new State();
        state.update(Map.of(StateKeys.DIMENSION, 3));

        generator.init(state);

        assertThrows(IllegalStateException.class, () -> generator.generate(new Random(1234L)));
    }

    @Test
    void metadata_isStable() {
        SingleBitFlipGenerator generator = new SingleBitFlipGenerator();

        assertEquals("single-bit-flip", generator.id());
        assertEquals("Single Bit Flip", generator.displayName());
        assertFalse(generator.description().isBlank());
        assertEquals(List.of("bitstring"), generator.supportedSearchSpaces());
        assertTrue(generator.params().isEmpty());
    }

    private static int countDifferences(boolean[] first, boolean[] second) {
        int count = 0;

        for (int i = 0; i < first.length; i++) {
            if (first[i] != second[i]) {
                count++;
            }
        }

        return count;
    }
}