package dk.dtu.scout.searchspace;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.searchSpace.BitString;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class BitStringTest {

    @Test
    void configure_setsDimension() {
        BitString searchSpace = new BitString();
        searchSpace.configure(Map.of("n", 5));
        assertEquals(5, searchSpace.dimension());
    }

    @Test
    void configure_rejectsNonPositiveDimension() {
        BitString searchSpace = new BitString();
        assertThrows(IllegalArgumentException.class, () -> searchSpace.configure(Map.of("n", 0)));
        assertThrows(IllegalArgumentException.class, () -> searchSpace.configure(Map.of("n", -1)));
    }

    @Test
    void randomSolution_returnsBitStringOfConfiguredLength() {
        BitString searchSpace = new BitString();
        searchSpace.configure(Map.of("n", 8));
        boolean[] solution = searchSpace.randomSolution(new Random(1234L));
        assertEquals(8, solution.length);
    }

    @Test
    void randomSolution_isDeterministicForSameSeed() {
        BitString searchSpace = new BitString();
        searchSpace.configure(Map.of("n", 8));

        boolean[] first = searchSpace.randomSolution(new Random(1234L));
        boolean[] second = searchSpace.randomSolution(new Random(1234L));

        assertArrayEquals(first, second);
    }

    @Test
    void init_writesDimensionToState() {
        BitString searchSpace = new BitString();
        searchSpace.configure(Map.of("n", 7));
        State state = new State();

        searchSpace.init(state);

        assertEquals(7, state.get(StateKeys.DIMENSION));
    }

    @Test
    void metadata_isStable() {
        BitString searchSpace = new BitString();

        assertEquals("bitstring", searchSpace.id());
        assertEquals("BitString", searchSpace.displayName());
        assertFalse(searchSpace.description().isBlank());
        assertEquals(1, searchSpace.params().size());
    }
}