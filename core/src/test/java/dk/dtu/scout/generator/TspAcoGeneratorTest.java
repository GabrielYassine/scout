package dk.dtu.scout.generator;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.datatypes.TSPInstance;
import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.problems.TSP;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
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
    void init_readsTspInstanceFromProblemState() {
        TspAcoGenerator generator = new TspAcoGenerator();
        State state = new State();

        TSP tsp = new TSP();
        tsp.configure(Map.of("tspInstance", tspInstance()));

        state.update(Map.of(StateKeys.PROBLEM, tsp));

        generator.init(state);

        int[] tour = generator.generate(new FixedIntAndDoubleRandom(0, 0.0, 0.0));

        assertEquals(3, tour.length);
        assertArrayEquals(new int[] {0, 1, 2}, sorted(tour));
    }

    @Test
    void generate_usesTspDistancesWhenTspInstanceIsConfigured() {
        TspAcoGenerator generator = new TspAcoGenerator();
        State state = new State();

        TSP tsp = new TSP();
        tsp.configure(Map.of("tspInstance", tspInstance()));

        state.update(Map.of(StateKeys.PROBLEM, tsp));

        generator.configure(Map.of(
                "alpha", 1.0,
                "beta", 2.0,
                "minPheromone", 1.0,
                "maxPheromone", 1.0
        ));

        generator.init(state);

        int[] tour = generator.generate(new FixedIntAndDoubleRandom(0, 0.0, 0.0));

        assertEquals(3, tour.length);
        assertArrayEquals(new int[] {0, 1, 2}, sorted(tour));
    }

    @Test
    void generate_reusesExistingPheromoneMatrixAfterFirstCall() {
        TspAcoGenerator generator = new TspAcoGenerator();
        State state = stateWithDimension(3);

        generator.init(state);

        int[] first = generator.generate(new FixedIntAndDoubleRandom(0, 0.0, 0.0));
        Object matrixAfterFirstGenerate = state.get(StateKeys.PHEROMONE_MATRIX);

        int[] second = generator.generate(new FixedIntAndDoubleRandom(1, 0.0, 0.0));
        Object matrixAfterSecondGenerate = state.get(StateKeys.PHEROMONE_MATRIX);

        assertEquals(3, first.length);
        assertEquals(3, second.length);
        assertSame(matrixAfterFirstGenerate, matrixAfterSecondGenerate);
    }

    @Test
    void getStateVariables_reusesExistingPheromoneMatrixAfterGenerate() {
        TspAcoGenerator generator = new TspAcoGenerator();
        State state = stateWithDimension(3);

        generator.init(state);
        generator.generate(new Random(1234L));

        Object matrixAfterGenerate = state.get(StateKeys.PHEROMONE_MATRIX);

        generator.getStateVariables(state);

        assertSame(matrixAfterGenerate, state.get(StateKeys.PHEROMONE_MATRIX));
    }

    @Test
    void generate_fallsBackToFirstCandidateWhenWeightsAreZero() {
        TspAcoGenerator generator = new TspAcoGenerator();
        State state = stateWithDimension(3);

        generator.configure(Map.of(
                "alpha", 5.0,
                "beta", 1.0,
                "minPheromone", 0.0,
                "maxPheromone", Double.MIN_VALUE
        ));

        generator.init(state);

        int[] tour = generator.generate(new FixedIntAndDoubleRandom(0, 0.99, 0.99));

        assertArrayEquals(new int[] {0, 1, 2}, tour);
    }

    @Test
    void generate_canSkipEarlyCityWhenThresholdIsHigh() {
        TspAcoGenerator generator = new TspAcoGenerator();
        State state = stateWithDimension(3);

        generator.configure(Map.of(
                "minPheromone", 1.0,
                "maxPheromone", 1.0,
                "alpha", 1.0,
                "beta", 1.0
        ));

        generator.init(state);

        int[] tour = generator.generate(new FixedIntAndDoubleRandom(0, 0.99, 0.0));

        assertEquals(0, tour[0]);
        assertEquals(2, tour[1]);
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
    void configure_rejectsNegativeReinforcementRate() {
        TspAcoGenerator generator = new TspAcoGenerator();

        assertThrows(IllegalArgumentException.class, () ->
                generator.configure(Map.of("reinforcementRate", -0.1))
        );
    }

    @Test
    void configure_ignoresNullParams() {
        TspAcoGenerator generator = new TspAcoGenerator();

        assertDoesNotThrow(() -> generator.configure(Map.of()));
    }

    @Test
    void configure_acceptsValidParameterValues() {
        TspAcoGenerator generator = new TspAcoGenerator();

        generator.configure(Map.of(
                "evaporationRate", 0.5,
                "reinforcementRate", 2.0,
                "alpha", 2.0,
                "beta", 3.0,
                "minPheromone", 0.1,
                "maxPheromone", 5.0,
                "acceptEqualFitness", false
        ));

        assertEquals(8, generator.params().size());
    }

    @Test
    void configure_rejectsNullReinforcementMode() {
        TspAcoGenerator generator = new TspAcoGenerator();
        Map<String, Object> params = new HashMap<>();

        params.put("reinforcementMode", null);

        assertThrows(IllegalArgumentException.class, () ->
                generator.configure(params)
        );
    }

    @Test
    void configure_rejectsInvalidReinforcementMode() {
        TspAcoGenerator generator = new TspAcoGenerator();

        assertThrows(IllegalArgumentException.class, () ->
                generator.configure(Map.of("reinforcementMode", "invalid-mode"))
        );
    }

    @Test
    void configure_acceptsLowercaseReinforcementModeWithWhitespace() {
        TspAcoGenerator generator = new TspAcoGenerator();
        State state = stateWithDimension(3);

        generator.configure(Map.of(
                "reinforcementMode", " iteration_best ",
                "evaporationRate", 0.0,
                "reinforcementRate", 1.0,
                "minPheromone", 0.0,
                "maxPheromone", 10.0
        ));

        generator.init(state);
        generator.generate(new Random(1234L));

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(
                new EvaluatedSolution<>(new int[] {0, 1, 2}, -3.0)
        )));

        double[][] matrix = (double[][]) generator.getStateVariables(state)
                .get(StateKeys.PHEROMONE_MATRIX);

        assertTrue(matrix[0][1] > 1.0);
        assertTrue(matrix[1][2] > 1.0);
        assertTrue(matrix[2][0] > 1.0);
    }

    @Test
    void configure_explicitReinforcementModeOverridesLegacyReinforceBestOnly() {
        TspAcoGenerator generator = new TspAcoGenerator();
        State state = stateWithDimension(3);

        generator.configure(Map.of(
                "reinforcementMode", "ALL",
                "reinforceBestOnly", true,
                "evaporationRate", 0.0,
                "reinforcementRate", 1.0,
                "minPheromone", 0.0,
                "maxPheromone", 10.0
        ));

        generator.init(state);
        generator.generate(new Random(1234L));

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(
                new EvaluatedSolution<>(new int[] {0, 1, 2}, -3.0),
                new EvaluatedSolution<>(new int[] {0, 2, 1}, -6.0)
        )));

        double[][] matrix = (double[][]) generator.getStateVariables(state)
                .get(StateKeys.PHEROMONE_MATRIX);

        assertTrue(matrix[0][1] > 1.0);
        assertTrue(matrix[0][2] > 1.0);
    }

    @Test
    void configure_legacyReinforceBestOnlyTrueUsesIterationBest() {
        TspAcoGenerator generator = new TspAcoGenerator();
        State state = stateWithDimension(3);

        generator.configure(Map.of(
                "reinforceBestOnly", true,
                "evaporationRate", 0.0,
                "reinforcementRate", 1.0,
                "minPheromone", 0.0,
                "maxPheromone", 10.0
        ));

        generator.init(state);
        generator.generate(new Random(1234L));

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(
                new EvaluatedSolution<>(new int[] {0, 1, 2}, -10.0),
                new EvaluatedSolution<>(new int[] {0, 2, 1}, -3.0)
        )));

        double[][] matrix = (double[][]) generator.getStateVariables(state)
                .get(StateKeys.PHEROMONE_MATRIX);

        assertTrue(matrix[0][2] > 1.0);
        assertTrue(matrix[2][1] > 1.0);
        assertTrue(matrix[1][0] > 1.0);
    }

    @Test
    void getStateVariables_doesNotUpdateWhenEvaluatedListIsMissing() {
        TspAcoGenerator generator = new TspAcoGenerator();
        State state = stateWithDimension(3);

        generator.configure(Map.of(
                "minPheromone", 0.0,
                "maxPheromone", 10.0
        ));
        generator.init(state);
        generator.generate(new Random(1234L));

        double[][] matrix = (double[][]) generator.getStateVariables(state)
                .get(StateKeys.PHEROMONE_MATRIX);

        assertEquals(1.0, matrix[0][1], 1e-9);
        assertEquals(1.0, matrix[1][2], 1e-9);
    }

    @Test
    void getStateVariables_doesNotUpdateWhenEvaluatedListIsEmpty() {
        TspAcoGenerator generator = new TspAcoGenerator();
        State state = stateWithDimension(3);

        generator.configure(Map.of(
                "minPheromone", 0.0,
                "maxPheromone", 10.0
        ));
        generator.init(state);
        generator.generate(new Random(1234L));

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of()));

        double[][] matrix = (double[][]) generator.getStateVariables(state)
                .get(StateKeys.PHEROMONE_MATRIX);

        assertEquals(1.0, matrix[0][1], 1e-9);
        assertEquals(1.0, matrix[1][2], 1e-9);
    }

    @Test
    void getStateVariables_bestSoFarDoesNotUpdateWhenBestCannotBeFound() {
        TspAcoGenerator generator = new TspAcoGenerator();
        State state = stateWithDimension(3);

        generator.configure(Map.of(
                "evaporationRate", 0.0,
                "reinforcementRate", 1.0,
                "minPheromone", 0.0,
                "maxPheromone", 10.0
        ));
        generator.init(state);
        generator.generate(new Random(1234L));

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(
                "not-an-evaluated-solution",
                new EvaluatedSolution<>("wrong-value-type", -3.0)
        )));

        double[][] matrix = (double[][]) generator.getStateVariables(state)
                .get(StateKeys.PHEROMONE_MATRIX);

        assertEquals(1.0, matrix[0][1], 1e-9);
        assertEquals(1.0, matrix[1][2], 1e-9);
    }

    @Test
    void getStateVariables_iterationBestDoesNotUpdateWhenBestCannotBeFound() {
        TspAcoGenerator generator = new TspAcoGenerator();
        State state = stateWithDimension(3);

        generator.configure(Map.of(
                "reinforcementMode", "ITERATION_BEST",
                "evaporationRate", 0.0,
                "reinforcementRate", 1.0,
                "minPheromone", 0.0,
                "maxPheromone", 10.0
        ));
        generator.init(state);
        generator.generate(new Random(1234L));

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(
                "not-an-evaluated-solution",
                new EvaluatedSolution<>("wrong-value-type", -3.0)
        )));

        double[][] matrix = (double[][]) generator.getStateVariables(state)
                .get(StateKeys.PHEROMONE_MATRIX);

        assertEquals(1.0, matrix[0][1], 1e-9);
        assertEquals(1.0, matrix[1][2], 1e-9);
    }

    @Test
    void getStateVariables_ignoresInvalidEntriesWhenReinforcingAll() {
        TspAcoGenerator generator = new TspAcoGenerator();
        State state = stateWithDimension(3);

        generator.configure(Map.of(
                "reinforcementMode", "ALL",
                "evaporationRate", 0.0,
                "reinforcementRate", 1.0,
                "minPheromone", 0.0,
                "maxPheromone", 10.0
        ));
        generator.init(state);
        generator.generate(new Random(1234L));

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(
                new EvaluatedSolution<>(new int[] {0, 1, 2}, -3.0),
                "not-an-evaluated-solution",
                new EvaluatedSolution<>("wrong-value-type", -3.0)
        )));

        double[][] matrix = (double[][]) generator.getStateVariables(state)
                .get(StateKeys.PHEROMONE_MATRIX);

        assertTrue(matrix[0][1] > 1.0);
        assertTrue(matrix[1][2] > 1.0);
        assertTrue(matrix[2][0] > 1.0);
    }

    @Test
    void bestSoFarAcceptsEqualFitnessByDefault() {
        TspAcoGenerator generator = new TspAcoGenerator();
        State state = stateWithDimension(4);

        generator.configure(Map.of(
                "evaporationRate", 0.0,
                "reinforcementRate", 1.0,
                "minPheromone", 0.0,
                "maxPheromone", 10.0
        ));
        generator.init(state);
        generator.generate(new Random(1234L));

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(
                new EvaluatedSolution<>(new int[] {0, 1, 2, 3}, -4.0)
        )));
        generator.getStateVariables(state);

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(
                new EvaluatedSolution<>(new int[] {0, 2, 1, 3}, -4.0)
        )));
        double[][] matrix = (double[][]) generator.getStateVariables(state)
                .get(StateKeys.PHEROMONE_MATRIX);

        assertTrue(matrix[0][2] > 1.0);
        assertTrue(matrix[1][3] > 1.0);
    }

    @Test
    void bestSoFarKeepsEarlierBestWhenCandidateIsWorse() {
        TspAcoGenerator generator = new TspAcoGenerator();
        State state = stateWithDimension(4);

        generator.configure(Map.of(
                "evaporationRate", 0.0,
                "reinforcementRate", 1.0,
                "minPheromone", 0.0,
                "maxPheromone", 10.0
        ));
        generator.init(state);
        generator.generate(new Random(1234L));

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(
                new EvaluatedSolution<>(new int[] {0, 1, 2, 3}, -4.0)
        )));
        generator.getStateVariables(state);

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(
                new EvaluatedSolution<>(new int[] {0, 2, 1, 3}, -5.0)
        )));
        double[][] matrix = (double[][]) generator.getStateVariables(state)
                .get(StateKeys.PHEROMONE_MATRIX);

        assertTrue(matrix[0][1] > matrix[0][2]);
        assertTrue(matrix[2][3] > matrix[1][3]);
    }

    @Test
    void bestOfKeepsEarlierBestWhenLaterFitnessIsNotHigher() {
        TspAcoGenerator generator = new TspAcoGenerator();
        State state = stateWithDimension(4);

        generator.configure(Map.of(
                "reinforcementMode", "ITERATION_BEST",
                "evaporationRate", 0.0,
                "reinforcementRate", 1.0,
                "minPheromone", 0.0,
                "maxPheromone", 10.0
        ));

        generator.init(state);
        generator.generate(new Random(1234L));

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(
                new EvaluatedSolution<>(new int[] {0, 1, 2, 3}, -4.0),
                new EvaluatedSolution<>(new int[] {0, 2, 1, 3}, -5.0)
        )));

        double[][] matrix = (double[][]) generator.getStateVariables(state)
                .get(StateKeys.PHEROMONE_MATRIX);

        assertTrue(matrix[0][1] > matrix[0][2]);
        assertTrue(matrix[2][3] > matrix[1][3]);
    }

    @Test
    void acceptEqualFitnessFalseKeepsEarlierBestWhenFitnessIsEqual() {
        TspAcoGenerator generator = new TspAcoGenerator();
        State state = stateWithDimension(4);

        generator.configure(Map.of(
                "acceptEqualFitness", false,
                "evaporationRate", 0.0,
                "reinforcementRate", 1.0,
                "minPheromone", 0.0,
                "maxPheromone", 10.0
        ));
        generator.init(state);
        generator.generate(new Random(1234L));

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(
                new EvaluatedSolution<>(new int[] {0, 1, 2, 3}, -4.0)
        )));
        generator.getStateVariables(state);

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(
                new EvaluatedSolution<>(new int[] {0, 2, 1, 3}, -4.0)
        )));
        double[][] matrix = (double[][]) generator.getStateVariables(state)
                .get(StateKeys.PHEROMONE_MATRIX);

        assertTrue(matrix[0][1] > matrix[0][2]);
        assertTrue(matrix[2][3] > matrix[1][3]);
    }

    @Test
    void acceptEqualFitnessFalseAcceptsStrictlyBetterCandidate() {
        TspAcoGenerator generator = new TspAcoGenerator();
        State state = stateWithDimension(4);

        generator.configure(Map.of(
                "acceptEqualFitness", false,
                "evaporationRate", 0.0,
                "reinforcementRate", 1.0,
                "minPheromone", 0.0,
                "maxPheromone", 10.0
        ));
        generator.init(state);
        generator.generate(new Random(1234L));

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(
                new EvaluatedSolution<>(new int[] {0, 1, 2, 3}, -5.0)
        )));
        generator.getStateVariables(state);

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(
                new EvaluatedSolution<>(new int[] {0, 2, 1, 3}, -4.0)
        )));
        double[][] matrix = (double[][]) generator.getStateVariables(state)
                .get(StateKeys.PHEROMONE_MATRIX);

        assertTrue(matrix[0][2] > 1.0);
        assertTrue(matrix[1][3] > 1.0);
    }

    @Test
    void depositPheromoneIgnoresNonPositiveTourLength() {
        TspAcoGenerator generator = new TspAcoGenerator();
        State state = stateWithDimension(3);

        generator.configure(Map.of(
                "evaporationRate", 0.0,
                "reinforcementRate", 1.0,
                "minPheromone", 0.0,
                "maxPheromone", 10.0
        ));
        generator.init(state);
        generator.generate(new Random(1234L));

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(
                new EvaluatedSolution<>(new int[] {0, 1, 2}, 1.0)
        )));

        double[][] matrix = (double[][]) generator.getStateVariables(state)
                .get(StateKeys.PHEROMONE_MATRIX);

        assertEquals(1.0, matrix[0][1], 1e-9);
        assertEquals(1.0, matrix[1][2], 1e-9);
    }

    @Test
    void depositPheromoneIgnoresNaNAndInfiniteTourLength() {
        TspAcoGenerator nanGenerator = new TspAcoGenerator();
        State nanState = stateWithDimension(3);

        nanGenerator.configure(Map.of(
                "evaporationRate", 0.0,
                "reinforcementRate", 1.0,
                "minPheromone", 0.0,
                "maxPheromone", 10.0
        ));
        nanGenerator.init(nanState);
        nanGenerator.generate(new Random(1234L));

        nanState.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(
                new EvaluatedSolution<>(new int[] {0, 1, 2}, Double.NaN)
        )));

        double[][] nanMatrix = (double[][]) nanGenerator.getStateVariables(nanState)
                .get(StateKeys.PHEROMONE_MATRIX);

        assertEquals(1.0, nanMatrix[0][1], 1e-9);

        TspAcoGenerator infiniteGenerator = new TspAcoGenerator();
        State infiniteState = stateWithDimension(3);

        infiniteGenerator.configure(Map.of(
                "evaporationRate", 0.0,
                "reinforcementRate", 1.0,
                "minPheromone", 0.0,
                "maxPheromone", 10.0
        ));
        infiniteGenerator.init(infiniteState);
        infiniteGenerator.generate(new Random(1234L));

        infiniteState.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(
                new EvaluatedSolution<>(new int[] {0, 1, 2}, Double.NEGATIVE_INFINITY)
        )));

        double[][] infiniteMatrix = (double[][]) infiniteGenerator.getStateVariables(infiniteState)
                .get(StateKeys.PHEROMONE_MATRIX);

        assertEquals(1.0, infiniteMatrix[0][1], 1e-9);
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
        assertEquals(8, generator.params().size());
        assertEquals(List.of("permutation"), generator.supportedSearchSpaces());
    }

    @Test
    void getStateVariables_initializesPheromoneMatrixWhenGenerateWasNotCalledFirst() {
        TspAcoGenerator generator = new TspAcoGenerator();
        State state = stateWithDimension(3);

        generator.init(state);

        Map<String, Object> variables = generator.getStateVariables(state);

        assertTrue(variables.get(StateKeys.PHEROMONE_MATRIX) instanceof double[][]);
        assertSame(variables.get(StateKeys.PHEROMONE_MATRIX), state.get(StateKeys.PHEROMONE_MATRIX));

        double[][] matrix = (double[][]) variables.get(StateKeys.PHEROMONE_MATRIX);

        assertEquals(3, matrix.length);
        assertEquals(3, matrix[0].length);
    }

    private static State stateWithDimension(int dimension) {
        State state = new State();
        state.update(Map.of(StateKeys.DIMENSION, dimension));
        return state;
    }

    private static TSPInstance tspInstance() {
        return new TSPInstance(
                "triangle",
                "test instance",
                3,
                new double[][] {
                        {0.0, 0.0},
                        {3.0, 4.0},
                        {6.0, 0.0}
                }
        );
    }

    private static int[] sorted(int[] values) {
        int[] copy = values.clone();
        Arrays.sort(copy);
        return copy;
    }

    private static final class FixedIntAndDoubleRandom extends Random {
        private final int firstInt;
        private final double[] doubles;
        private int doubleIndex;

        private FixedIntAndDoubleRandom(int firstInt, double... doubles) {
            this.firstInt = firstInt;
            this.doubles = doubles;
        }

        @Override
        public int nextInt(int bound) {
            return Math.floorMod(firstInt, bound);
        }

        @Override
        public double nextDouble() {
            if (doubles.length == 0) {
                return 0.0;
            }

            return doubles[doubleIndex++ % doubles.length];
        }
    }
}