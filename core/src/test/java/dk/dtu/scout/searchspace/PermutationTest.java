package dk.dtu.scout.searchspace;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.searchSpace.Permutation;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class PermutationTest {

    @Test
    void configure_setsDimension() {
        Permutation searchSpace = new Permutation();

        searchSpace.configure(Map.of("n", 5));

        assertEquals(5, searchSpace.dimension());
    }

    @Test
    void configure_rejectsNonPositiveDimension() {
        Permutation searchSpace = new Permutation();

        assertThrows(IllegalArgumentException.class, () -> searchSpace.configure(Map.of("n", 0)));
        assertThrows(IllegalArgumentException.class, () -> searchSpace.configure(Map.of("n", -1)));
    }

    @Test
    void randomSolution_returnsPermutationOfConfiguredRange() {
        Permutation searchSpace = new Permutation();
        searchSpace.configure(Map.of("n", 6));

        int[] solution = searchSpace.randomSolution(new Random(1234L));

        assertEquals(6, solution.length);
        assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5}, sorted(solution));
    }

    @Test
    void randomSolution_isDeterministicForSameSeed() {
        Permutation searchSpace = new Permutation();
        searchSpace.configure(Map.of("n", 6));

        int[] first = searchSpace.randomSolution(new Random(1234L));
        int[] second = searchSpace.randomSolution(new Random(1234L));

        assertArrayEquals(first, second);
    }

    @Test
    void init_writesDimensionToState() {
        Permutation searchSpace = new Permutation();
        searchSpace.configure(Map.of("n", 7));
        State state = new State();

        searchSpace.init(state);

        assertEquals(7, state.get(StateKeys.DIMENSION));
    }

    @Test
    void metadata_isStable() {
        Permutation searchSpace = new Permutation();

        assertEquals("permutation", searchSpace.id());
        assertEquals("Permutation", searchSpace.displayName());
        assertFalse(searchSpace.description().isBlank());
        assertEquals(1, searchSpace.params().size());
    }

    private static int[] sorted(int[] values) {
        int[] copy = values.clone();
        Arrays.sort(copy);
        return copy;
    }
}