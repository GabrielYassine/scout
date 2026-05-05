package dk.dtu.scout.generator;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.dto.EvaluatedSolution;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class TspAcoGeneratorTest {

    @Test
    void generate_constructsPermutationOfConfiguredDimension() {
        TspAcoGenerator generator = new TspAcoGenerator();

        generator.init(stateWithDimension(4));

        int[] tour = generator.generate(new Random(1234L));

        assertEquals(4, tour.length);
        assertArrayEquals(new int[] {0, 1, 2, 3}, sorted(tour));
    }

    @Test
    void getStateVariables_updatesAndReturnsPheromoneMatrix() {
        TspAcoGenerator generator = new TspAcoGenerator();
        State state = stateWithDimension(3);

        generator.configure(Map.of(
            "evaporationRate", 0.0,
            "minPheromone", 0.0,
            "maxPheromone", 10.0
        ));
        generator.init(state);
        generator.generate(new Random(1234L));

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(
            new EvaluatedSolution<>(new int[] {0, 1, 2}, -3.0)
        )));

        Map<String, Object> vars = generator.getStateVariables(state);
        double[][] matrix = (double[][]) vars.get(StateKeys.PHEROMONE_MATRIX);

        assertTrue(matrix[0][1] > 1.0);
        assertTrue(matrix[1][2] > 1.0);
        assertTrue(matrix[2][0] > 1.0);
        assertSame(matrix, state.get(StateKeys.PHEROMONE_MATRIX));
    }

    @Test
    void getStateVariables_canReinforceAllTours() {
        TspAcoGenerator generator = new TspAcoGenerator();
        State state = stateWithDimension(3);

        generator.configure(Map.of(
            "evaporationRate", 0.0,
            "minPheromone", 0.0,
            "maxPheromone", 10.0,
            "reinforceBestOnly", false
        ));
        generator.init(state);
        generator.generate(new Random(1234L));

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(
            new EvaluatedSolution<>(new int[] {0, 1, 2}, -3.0),
            new EvaluatedSolution<>(new int[] {0, 2, 1}, -6.0)
        )));

        double[][] matrix = (double[][]) generator.getStateVariables(state).get(StateKeys.PHEROMONE_MATRIX);

        assertTrue(matrix[0][1] > 1.0);
        assertTrue(matrix[0][2] > 1.0);
    }

    @Test
    void configure_rejectsInvalidValues() {
        TspAcoGenerator generator = new TspAcoGenerator();

        assertThrows(IllegalArgumentException.class, () -> generator.configure(Map.of("evaporationRate", -0.1)));
        assertThrows(IllegalArgumentException.class, () -> generator.configure(Map.of("evaporationRate", 1.1)));
        assertThrows(IllegalArgumentException.class, () -> generator.configure(Map.of("alpha", 0.0)));
        assertThrows(IllegalArgumentException.class, () -> generator.configure(Map.of("alpha", 6.0)));
        assertThrows(IllegalArgumentException.class, () -> generator.configure(Map.of("beta", 0.0)));
        assertThrows(IllegalArgumentException.class, () -> generator.configure(Map.of("beta", 11.0)));
        assertThrows(IllegalArgumentException.class, () -> generator.configure(Map.of("minPheromone", -0.1)));
        assertThrows(IllegalArgumentException.class, () -> generator.configure(Map.of("maxPheromone", 0.0)));
        assertThrows(IllegalArgumentException.class, () -> generator.configure(Map.of(
            "minPheromone", 2.0,
            "maxPheromone", 1.0
        )));
    }

    @Test
    void generate_rejectsInvalidDimension() {
        TspAcoGenerator generator = new TspAcoGenerator();

        generator.init(new State());

        assertThrows(IllegalStateException.class, () -> generator.generate(new Random(1234L)));
    }

    @Test
    void metadata_isStable() {
        TspAcoGenerator generator = new TspAcoGenerator();

        assertEquals("tsp-aco", generator.id());
        assertEquals("TSP ACO Generator", generator.displayName());
        assertFalse(generator.description().isBlank());
        assertEquals(6, generator.params().size());
        assertEquals(List.of("permutation"), generator.supportedSearchSpaces());
    }

    private static State stateWithDimension(int dimension) {
        State state = new State();
        state.update(Map.of(StateKeys.DIMENSION, dimension));
        return state;
    }

    private static int[] sorted(int[] values) {
        int[] copy = values.clone();
        Arrays.sort(copy);
        return copy;
    }
}