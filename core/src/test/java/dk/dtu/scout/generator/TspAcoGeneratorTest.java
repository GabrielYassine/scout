package dk.dtu.scout.generator;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.datatypes.TSPInstance;
import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.problems.TSP;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TspAcoGeneratorTest {

    @Nested
    class Generation {

        @Test
        void generate_constructsPermutationOfConfiguredDimension() {
            TspAcoGenerator generator = new TspAcoGenerator();

            generator.init(stateWithDimension(4));

            int[] tour = generator.generate(new Random(1234L));

            assertEquals(4, tour.length);
            assertArrayEquals(new int[] {0, 1, 2, 3}, sorted(tour));
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
    }

    @Nested
    class PheromoneState {

        @Test
        void getStateVariables_initializesPheromoneMatrixWhenGenerateWasNotCalledFirst() {
            TspAcoGenerator generator = new TspAcoGenerator();
            State state = stateWithDimension(3);

            generator.init(state);

            Map<String, Object> variables = generator.getStateVariables(state);

            assertInstanceOf(double[][].class, variables.get(StateKeys.PHEROMONE_MATRIX));
            assertSame(variables.get(StateKeys.PHEROMONE_MATRIX), state.get(StateKeys.PHEROMONE_MATRIX));

            double[][] matrix = (double[][]) variables.get(StateKeys.PHEROMONE_MATRIX);

            assertEquals(3, matrix.length);
            assertEquals(3, matrix[0].length);
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

            state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(new EvaluatedSolution<>(new int[]{0, 1, 2}, -3.0))));

            Map<String, Object> vars = generator.getStateVariables(state);
            double[][] matrix = (double[][]) vars.get(StateKeys.PHEROMONE_MATRIX);

            assertTrue(matrix[0][1] > 1.0);
            assertTrue(matrix[1][2] > 1.0);
            assertTrue(matrix[2][0] > 1.0);
            assertSame(matrix, state.get(StateKeys.PHEROMONE_MATRIX));
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

            double[][] matrix = (double[][]) generator.getStateVariables(state).get(StateKeys.PHEROMONE_MATRIX);

            assertEquals(1.0, matrix[0][1], 1e-9);
            assertEquals(1.0, matrix[1][2], 1e-9);
        }
    }

    @Nested
    class ReinforcementModes {

        @Test
        void getStateVariables_canReinforceAllTours() {
            TspAcoGenerator generator = new TspAcoGenerator();
            State state = stateWithDimension(3);

            generator.configure(Map.of(
                "reinforcementMode", "ALL",
                "evaporationRate", 0.0,
                "minPheromone", 0.0,
                "maxPheromone", 10.0
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

            state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(new EvaluatedSolution<>(new int[]{0, 1, 2}, -3.0))));
            double[][] matrix = (double[][]) generator.getStateVariables(state).get(StateKeys.PHEROMONE_MATRIX);

            assertTrue(matrix[0][1] > 1.0);
            assertTrue(matrix[1][2] > 1.0);
            assertTrue(matrix[2][0] > 1.0);
        }

        @Test
        void iterationBestSelectsLaterTourWhenItHasHigherFitness() {
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

            state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(new EvaluatedSolution<>(new int[]{0, 1, 2, 3}, -5.0), new EvaluatedSolution<>(new int[]{0, 2, 1, 3}, -4.0))));

            double[][] matrix = (double[][]) generator.getStateVariables(state).get(StateKeys.PHEROMONE_MATRIX);

            assertTrue(matrix[0][2] > 1.0);
            assertTrue(matrix[2][1] > 1.0);
            assertTrue(matrix[1][3] > 1.0);
            assertTrue(matrix[3][0] > 1.0);
        }

        @Test
        void iterationBestKeepsEarlierTourWhenLaterTourHasWorseFitness() {
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

            state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(new EvaluatedSolution<>(new int[]{0, 1, 2, 3}, -4.0), new EvaluatedSolution<>(new int[]{0, 2, 1, 3}, -5.0))));

            double[][] matrix = (double[][]) generator.getStateVariables(state).get(StateKeys.PHEROMONE_MATRIX);

            assertTrue(matrix[0][1] > matrix[0][2]);
            assertTrue(matrix[2][3] > matrix[1][3]);
        }
    }

    @Nested
    class BestSoFarBehaviour {

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

            state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(new EvaluatedSolution<>(new int[]{0, 1, 2, 3}, -4.0))));
            generator.getStateVariables(state);
            state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(new EvaluatedSolution<>(new int[]{0, 2, 1, 3}, -4.0))));
            double[][] matrix = (double[][]) generator.getStateVariables(state).get(StateKeys.PHEROMONE_MATRIX);

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

            state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(new EvaluatedSolution<>(new int[]{0, 1, 2, 3}, -4.0))));
            generator.getStateVariables(state);

            state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(new EvaluatedSolution<>(new int[]{0, 2, 1, 3}, -5.0))));
            double[][] matrix = (double[][]) generator.getStateVariables(state).get(StateKeys.PHEROMONE_MATRIX);

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

            state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(new EvaluatedSolution<>(new int[]{0, 1, 2, 3}, -4.0))));
            generator.getStateVariables(state);

            state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(new EvaluatedSolution<>(new int[]{0, 2, 1, 3}, -4.0))));
            double[][] matrix = (double[][]) generator.getStateVariables(state).get(StateKeys.PHEROMONE_MATRIX);

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

            state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(new EvaluatedSolution<>(new int[]{0, 1, 2, 3}, -5.0))));
            generator.getStateVariables(state);

            state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(new EvaluatedSolution<>(new int[]{0, 2, 1, 3}, -4.0))));
            double[][] matrix = (double[][]) generator.getStateVariables(state).get(StateKeys.PHEROMONE_MATRIX);

            assertTrue(matrix[0][2] > 1.0);
            assertTrue(matrix[1][3] > 1.0);
        }
    }

    @Nested
    class ConfigurationValidation {

        @ParameterizedTest(name = "{0}")
        @MethodSource("dk.dtu.scout.generator.TspAcoGeneratorTest#invalidConfigurations")
        void configure_rejectsInvalidValues(String label, Map<String, Object> params) {
            TspAcoGenerator generator = new TspAcoGenerator();
            assertThrows(IllegalArgumentException.class, () -> generator.configure(params));
        }

        @Test
        void configure_acceptsValidParameterValuesWithoutThrowing() {
            TspAcoGenerator generator = new TspAcoGenerator();

            assertDoesNotThrow(() -> generator.configure(Map.of(
                "evaporationRate", 0.5,
                "reinforcementRate", 2.0,
                "alpha", 2.0,
                "beta", 3.0,
                "minPheromone", 0.1,
                "maxPheromone", 5.0,
                "acceptEqualFitness", false
            )));
        }

        @Test
        void configure_rejectsInvalidReinforcementMode() {
            TspAcoGenerator generator = new TspAcoGenerator();
            assertThrows(IllegalArgumentException.class, () -> generator.configure(Map.of("reinforcementMode", "invalid-mode")));
        }
    }

    @Nested
    class Metadata {

        @Test
        void metadata_isStable() {
            TspAcoGenerator generator = new TspAcoGenerator();

            assertEquals("tsp-aco", generator.id());
            assertEquals("TSP ACO Generator", generator.displayName());
            assertFalse(generator.description().isBlank());
            assertEquals(8, generator.params().size());
            assertEquals(List.of("permutation"), generator.supportedSearchSpaces());
        }
    }

    private static Stream<Arguments> invalidConfigurations() {
        return Stream.of(
            Arguments.of("negative evaporation rate", Map.of("evaporationRate", -0.1)),
            Arguments.of("evaporation rate above one", Map.of("evaporationRate", 1.1)),
            Arguments.of("zero alpha", Map.of("alpha", 0.0)),
            Arguments.of("alpha too large", Map.of("alpha", 6.0)),
            Arguments.of("zero beta", Map.of("beta", 0.0)),
            Arguments.of("beta too large", Map.of("beta", 11.0)),
            Arguments.of("negative min pheromone", Map.of("minPheromone", -0.1)),
            Arguments.of("zero max pheromone", Map.of("maxPheromone", 0.0)),
            Arguments.of("min pheromone greater than max pheromone", Map.of("minPheromone", 2.0, "maxPheromone", 1.0)),
            Arguments.of("negative reinforcement rate", Map.of("reinforcementRate", -0.1))
        );
    }

    private static State stateWithDimension(int dimension) {
        State state = new State();

        TSP tsp = new TSP();
        tsp.configure(Map.of("tspInstance", tspInstance(dimension)));

        state.update(Map.of(
            StateKeys.PROBLEM, tsp,
            StateKeys.DIMENSION, dimension
        ));

        return state;
    }

    private static TSPInstance tspInstance(int dimension) {
        double[][] coordinates = new double[dimension][2];

        for (int i = 0; i < dimension; i++) {
            coordinates[i][0] = i;
            coordinates[i][1] = 0.0;
        }

        return new TSPInstance(
            "line-" + dimension,
            "test instance",
            dimension,
            coordinates
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