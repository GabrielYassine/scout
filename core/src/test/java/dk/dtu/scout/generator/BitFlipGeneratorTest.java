package dk.dtu.scout.generator;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static dk.dtu.scout.generator.GeneratorTestSupport.stateWithBase;
import static org.junit.jupiter.api.Assertions.*;

class BitFlipGeneratorTest {

    @Test
    void generate_withProbabilityZeroKeepsAllBits() {
        BitFlipGenerator generator = new BitFlipGenerator();
        boolean[] base = new boolean[] {true, false, true};

        generator.configure(Map.of("flipProbability", 0.0));
        generator.init(stateWithBase(base));

        boolean[] result = generator.generate(new Random(1234L));

        assertArrayEquals(base, result);
        assertNotSame(base, result);
    }

    @Test
    void generate_withProbabilityOneFlipsAllBits() {
        BitFlipGenerator generator = new BitFlipGenerator();
        boolean[] base = new boolean[] {true, false, true};

        generator.configure(Map.of("flipProbability", 1.0));
        generator.init(stateWithBase(base));

        boolean[] result = generator.generate(new Random(1234L));

        assertArrayEquals(new boolean[] {false, true, false}, result);
    }

    @Test
    void generate_resolvesFormulaUsingDimension() {
        BitFlipGenerator generator = new BitFlipGenerator();
        boolean[] base = new boolean[] {true, true, true, true};

        generator.configure(Map.of("flipProbability", "1/n"));
        generator.init(stateWithBase(base));

        boolean[] result = generator.generate(new Random(1234L));

        assertEquals(4, result.length);
    }

    @Test
    void generate_clampsNegativeProbabilityToZero() {
        BitFlipGenerator generator = new BitFlipGenerator();
        boolean[] base = new boolean[] {true, false, true};

        generator.configure(Map.of("flipProbability", -1.0));
        generator.init(stateWithBase(base));

        boolean[] result = generator.generate(new Random(1234L));

        assertArrayEquals(base, result);
    }

    @Test
    void generate_clampsProbabilityAboveOneToOne() {
        BitFlipGenerator generator = new BitFlipGenerator();
        boolean[] base = new boolean[] {true, false, true};

        generator.configure(Map.of("flipProbability", 2.0));
        generator.init(stateWithBase(base));

        boolean[] result = generator.generate(new Random(1234L));

        assertArrayEquals(new boolean[] {false, true, false}, result);
    }

    @Test
    void generate_withEmptyBitstringReturnsSameEmptyArray() {
        BitFlipGenerator generator = new BitFlipGenerator();
        boolean[] base = new boolean[0];

        generator.configure(Map.of("flipProbability", 1.0));
        generator.init(stateWithBase(base));

        boolean[] result = generator.generate(new Random(1234L));

        assertSame(base, result);
    }

    @Test
    void generate_rejectsMissingOffspringBase() {
        BitFlipGenerator generator = new BitFlipGenerator();

        State state = new State();
        state.update(Map.of(StateKeys.DIMENSION, 3));

        generator.init(state);

        assertThrows(IllegalStateException.class, () -> generator.generate(new Random(1234L)));
    }

    @Test
    void metadata_isStable() {
        BitFlipGenerator generator = new BitFlipGenerator();

        assertEquals("bit-flip", generator.id());
        assertEquals("Bit Flip (p)", generator.displayName());
        assertFalse(generator.description().isBlank());
        assertEquals(List.of("bitstring"), generator.supportedSearchSpaces());
        assertEquals(1, generator.params().size());
    }
}