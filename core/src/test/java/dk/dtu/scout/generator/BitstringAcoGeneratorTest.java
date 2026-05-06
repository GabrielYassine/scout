package dk.dtu.scout.generator;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.dto.EvaluatedSolution;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class BitstringAcoGeneratorTest {

    @Test
    void generate_initializesPheromonesAndSamplesBitstring() {
        BitstringAcoGenerator generator = new BitstringAcoGenerator();

        generator.init(stateWithDimension(4));
        generator.configure(Map.of("minPheromone", 0.0, "maxPheromone", 1.0));

        boolean[] result = generator.generate(new FixedDoubleRandom(0.25, 0.75, 0.25, 0.75));

        assertArrayEquals(new boolean[] {true, false, true, false}, result);
    }

    @Test
    void generate_resolvesStringPheromoneBounds() {
        BitstringAcoGenerator generator = new BitstringAcoGenerator();
        State state = stateWithDimension(4);

        generator.configure(Map.of("minPheromone", "1/n", "maxPheromone", "1 - 1/n"));
        generator.init(state);

        boolean[] result = generator.generate(new FixedDoubleRandom(0.2, 0.8, 0.2, 0.8));

        assertArrayEquals(new boolean[] {true, false, true, false}, result);
    }

    @Test
    void generate_rejectsInvalidPheromoneBoundType() {
        BitstringAcoGenerator generator = new BitstringAcoGenerator();
        State state = stateWithDimension(4);

        generator.configure(Map.of("minPheromone", true));
        generator.init(state);

        assertThrows(IllegalArgumentException.class, () -> generator.generate(new Random(1234L)));
    }

    @Test
    void getStateVariables_initializesPheromonesWhenGenerateWasNotCalledFirst() {
        BitstringAcoGenerator generator = new BitstringAcoGenerator();
        State state = stateWithDimension(3);

        generator.init(state);

        Map<String, Object> vars = generator.getStateVariables(state);

        assertTrue(vars.get(StateKeys.PHEROMONE_VECTOR) instanceof double[]);
        assertSame(vars.get(StateKeys.PHEROMONE_VECTOR), state.get(StateKeys.PHEROMONE_VECTOR));
    }

    @Test
    void generate_rejectsNonFinitePheromoneBounds() {
        BitstringAcoGenerator nanGenerator = new BitstringAcoGenerator();
        nanGenerator.configure(Map.of("minPheromone", Double.NaN));
        nanGenerator.init(stateWithDimension(4));

        assertThrows(IllegalArgumentException.class, () -> nanGenerator.generate(new Random(1234L)));

        BitstringAcoGenerator infiniteGenerator = new BitstringAcoGenerator();
        infiniteGenerator.configure(Map.of("maxPheromone", Double.POSITIVE_INFINITY));
        infiniteGenerator.init(stateWithDimension(4));

        assertThrows(IllegalArgumentException.class, () -> infiniteGenerator.generate(new Random(1234L)));
    }

    @Test
    void configure_allowsPartialConfigurationWithoutChangingDefaultBounds() {
        BitstringAcoGenerator generator = new BitstringAcoGenerator();
        State state = stateWithDimension(4);

        generator.configure(Map.of("evaporationRate", 0.0));
        generator.init(state);

        boolean[] result = generator.generate(new FixedDoubleRandom(0.1, 0.9, 0.1, 0.9));

        assertArrayEquals(new boolean[] {true, false, true, false}, result);
    }

    @Test
    void getStateVariables_reinforcesBestSolutionOnlyByDefault() {
        BitstringAcoGenerator generator = new BitstringAcoGenerator();
        State state = stateWithDimension(3);

        generator.configure(Map.of(
            "evaporationRate", 0.0,
            "reinforcementRate", 0.2,
            "minPheromone", 0.0,
            "maxPheromone", 1.0
        ));
        generator.init(state);
        generator.generate(new FixedDoubleRandom(1.0, 1.0, 1.0));

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(
            new EvaluatedSolution<>(new boolean[] {true, false, true}, 10.0),
            new EvaluatedSolution<>(new boolean[] {false, true, false}, 5.0)
        )));

        Map<String, Object> vars = generator.getStateVariables(state);
        double[] pheromones = (double[]) vars.get(StateKeys.PHEROMONE_VECTOR);

        assertArrayEquals(new double[] {0.7, 0.5, 0.7}, pheromones, 1e-9);
        assertSame(pheromones, state.get(StateKeys.PHEROMONE_VECTOR));
    }

    @Test
    void configure_rejectsInvalidRates() {
        BitstringAcoGenerator generator = new BitstringAcoGenerator();

        assertThrows(IllegalArgumentException.class, () -> generator.configure(Map.of("evaporationRate", -0.1)));
        assertThrows(IllegalArgumentException.class, () -> generator.configure(Map.of("evaporationRate", 1.1)));
        assertThrows(IllegalArgumentException.class, () -> generator.configure(Map.of("reinforcementRate", -0.1)));
        assertThrows(IllegalArgumentException.class, () -> generator.configure(Map.of("reinforcementRate", 1.1)));
    }

    @Test
    void generate_rejectsInvalidPheromoneBounds() {
        BitstringAcoGenerator generator = new BitstringAcoGenerator();

        generator.init(stateWithDimension(4));
        generator.configure(Map.of("minPheromone", 0.8, "maxPheromone", 0.2));

        assertThrows(IllegalArgumentException.class, () -> generator.generate(new Random(1234L)));
    }

    @Test
    void metadata_isStable() {
        BitstringAcoGenerator generator = new BitstringAcoGenerator();

        assertEquals("bitstring-aco", generator.id());
        assertEquals("Bitstring ACO Generator", generator.displayName());
        assertFalse(generator.description().isBlank());
        assertEquals(6, generator.params().size());
        assertEquals(List.of("bitstring"), generator.supportedSearchSpaces());
    }

    @Test
    void generate_reusesExistingPheromoneVectorAfterFirstCall() {
        BitstringAcoGenerator generator = new BitstringAcoGenerator();
        State state = stateWithDimension(4);

        generator.configure(Map.of("minPheromone", 0.5, "maxPheromone", 0.5));
        generator.init(state);

        boolean[] first = generator.generate(new FixedDoubleRandom(0.25, 0.25, 0.25, 0.25));
        Object vectorAfterFirstGenerate = state.get(StateKeys.PHEROMONE_VECTOR);

        boolean[] second = generator.generate(new FixedDoubleRandom(0.75, 0.75, 0.75, 0.75));
        Object vectorAfterSecondGenerate = state.get(StateKeys.PHEROMONE_VECTOR);

        assertArrayEquals(new boolean[] {true, true, true, true}, first);
        assertArrayEquals(new boolean[] {false, false, false, false}, second);
        assertSame(vectorAfterFirstGenerate, vectorAfterSecondGenerate);
    }

    @Test
    void generate_rejectsPheromoneBoundsOutsideZeroOneRange() {
        BitstringAcoGenerator negativeMin = new BitstringAcoGenerator();
        negativeMin.configure(Map.of("minPheromone", -0.1));
        negativeMin.init(stateWithDimension(4));

        assertThrows(IllegalArgumentException.class, () -> negativeMin.generate(new Random(1234L)));

        BitstringAcoGenerator tooLargeMin = new BitstringAcoGenerator();
        tooLargeMin.configure(Map.of("minPheromone", 1.1));
        tooLargeMin.init(stateWithDimension(4));

        assertThrows(IllegalArgumentException.class, () -> tooLargeMin.generate(new Random(1234L)));

        BitstringAcoGenerator negativeMax = new BitstringAcoGenerator();
        negativeMax.configure(Map.of("maxPheromone", -0.1));
        negativeMax.init(stateWithDimension(4));

        assertThrows(IllegalArgumentException.class, () -> negativeMax.generate(new Random(1234L)));

        BitstringAcoGenerator tooLargeMax = new BitstringAcoGenerator();
        tooLargeMax.configure(Map.of("maxPheromone", 1.1));
        tooLargeMax.init(stateWithDimension(4));

        assertThrows(IllegalArgumentException.class, () -> tooLargeMax.generate(new Random(1234L)));
    }

    @Test
    void getStateVariables_doesNotUpdatePheromonesWhenEvaluatedListIsEmpty() {
        BitstringAcoGenerator generator = new BitstringAcoGenerator();
        State state = stateWithDimension(3);

        generator.configure(Map.of("minPheromone", 0.0, "maxPheromone", 1.0));
        generator.init(state);
        generator.generate(new FixedDoubleRandom(1.0, 1.0, 1.0));

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of()));

        double[] pheromones = (double[]) generator.getStateVariables(state).get(StateKeys.PHEROMONE_VECTOR);

        assertArrayEquals(new double[] {0.5, 0.5, 0.5}, pheromones, 1e-9);
    }

    @Test
    void configure_usesIterationBestMode() {
        BitstringAcoGenerator generator = new BitstringAcoGenerator();
        State state = stateWithDimension(3);

        generator.configure(Map.of(
            "reinforcementMode", " iteration_best ",
            "evaporationRate", 0.0,
            "reinforcementRate", 0.2,
            "minPheromone", 0.0,
            "maxPheromone", 1.0
        ));

        generator.init(state);
        generator.generate(new FixedDoubleRandom(1.0, 1.0, 1.0));

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(
                new EvaluatedSolution<>(new boolean[] {true, false, false}, 1.0),
                new EvaluatedSolution<>(new boolean[] {false, true, true}, 10.0)
        )));

        double[] pheromones = (double[]) generator.getStateVariables(state).get(StateKeys.PHEROMONE_VECTOR);

        assertArrayEquals(new double[] {0.5, 0.7, 0.7}, pheromones, 1e-9);
    }

    @Test
    void bestSoFarAcceptsEqualFitnessByDefault() {
        BitstringAcoGenerator generator = new BitstringAcoGenerator();
        State state = stateWithDimension(3);

        generator.configure(Map.of(
            "evaporationRate", 0.0,
            "reinforcementRate", 0.2,
            "minPheromone", 0.0,
            "maxPheromone", 1.0
        ));

        generator.init(state);
        generator.generate(new FixedDoubleRandom(1.0, 1.0, 1.0));

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(new EvaluatedSolution<>(new boolean[] {true, false, false}, 10.0))));

        generator.getStateVariables(state);

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(new EvaluatedSolution<>(new boolean[] {false, true, true}, 10.0))));

        double[] pheromones = (double[]) generator.getStateVariables(state).get(StateKeys.PHEROMONE_VECTOR);

        assertArrayEquals(new double[] {0.7, 0.7, 0.7}, pheromones, 1e-9);
    }

    @Test
    void configure_rejectsInvalidReinforcementMode() {
        BitstringAcoGenerator generator = new BitstringAcoGenerator();
        assertThrows(IllegalArgumentException.class, () -> generator.configure(Map.of("reinforcementMode", "invalid-mode")));
    }

    @Test
    void configure_usesAllReinforcementMode() {
        BitstringAcoGenerator generator = new BitstringAcoGenerator();
        State state = stateWithDimension(3);

        generator.configure(Map.of(
            "reinforcementMode", "ALL",
            "evaporationRate", 0.0,
            "reinforcementRate", 0.3,
            "minPheromone", 0.0,
            "maxPheromone", 1.0
        ));

        generator.init(state);
        generator.generate(new FixedDoubleRandom(1.0, 1.0, 1.0));

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(
            new EvaluatedSolution<>(new boolean[] {true, false, false}, 10.0),
            new EvaluatedSolution<>(new boolean[] {false, true, false}, 5.0)
        )));

        double[] pheromones = (double[]) generator.getStateVariables(state).get(StateKeys.PHEROMONE_VECTOR);

        assertArrayEquals(new double[] {0.65, 0.65, 0.5}, pheromones, 1e-9);
    }

    @Test
    void bestSoFarKeepsEarlierBestWhenCandidateIsWorse() {
        BitstringAcoGenerator generator = new BitstringAcoGenerator();
        State state = stateWithDimension(3);

        generator.configure(Map.of(
            "evaporationRate", 0.0,
            "reinforcementRate", 0.2,
            "minPheromone", 0.0,
            "maxPheromone", 1.0
        ));

        generator.init(state);
        generator.generate(new FixedDoubleRandom(1.0, 1.0, 1.0));

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(new EvaluatedSolution<>(new boolean[] {true, false, false}, 10.0))));
        generator.getStateVariables(state);

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(new EvaluatedSolution<>(new boolean[] {false, true, true}, 9.0))));
        double[] pheromones = (double[]) generator.getStateVariables(state).get(StateKeys.PHEROMONE_VECTOR);

        assertArrayEquals(new double[] {0.9, 0.5, 0.5}, pheromones, 1e-9);
    }

    @Test
    void acceptEqualFitnessFalseAcceptsStrictlyBetterCandidate() {
        BitstringAcoGenerator generator = new BitstringAcoGenerator();
        State state = stateWithDimension(3);

        generator.configure(Map.of(
            "acceptEqualFitness", false,
            "evaporationRate", 0.0,
            "reinforcementRate", 0.2,
            "minPheromone", 0.0,
            "maxPheromone", 1.0
        ));

        generator.init(state);
        generator.generate(new FixedDoubleRandom(1.0, 1.0, 1.0));

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(new EvaluatedSolution<>(new boolean[] {true, false, false}, 10.0))));
        generator.getStateVariables(state);

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(new EvaluatedSolution<>(new boolean[] {false, true, true}, 11.0))));
        double[] pheromones = (double[]) generator.getStateVariables(state).get(StateKeys.PHEROMONE_VECTOR);

        assertArrayEquals(new double[] {0.7, 0.7, 0.7}, pheromones, 1e-9);
    }

    @Test
    void acceptEqualFitnessFalseKeepsEarlierBestWhenCandidateHasEqualFitness() {
        BitstringAcoGenerator generator = new BitstringAcoGenerator();
        State state = stateWithDimension(3);

        generator.configure(Map.of(
            "acceptEqualFitness", false,
            "evaporationRate", 0.0,
            "reinforcementRate", 0.2,
            "minPheromone", 0.0,
            "maxPheromone", 1.0
        ));

        generator.init(state);
        generator.generate(new FixedDoubleRandom(1.0, 1.0, 1.0));

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(new EvaluatedSolution<>(new boolean[] {true, false, false}, 10.0))));
        generator.getStateVariables(state);

        state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.of(new EvaluatedSolution<>(new boolean[] {false, true, true}, 10.0))));

        double[] pheromones = (double[]) generator.getStateVariables(state).get(StateKeys.PHEROMONE_VECTOR);
        assertArrayEquals(new double[] {0.9, 0.5, 0.5}, pheromones, 1e-9);
    }

    private static State stateWithDimension(int dimension) {
        State state = new State();
        state.update(Map.of(StateKeys.DIMENSION, dimension));
        return state;
    }

    private static final class FixedDoubleRandom extends Random {
        private final double[] values;
        private int index;

        private FixedDoubleRandom(double... values) {
            this.values = values;
        }

        @Override
        public double nextDouble() {
            return values[index++ % values.length];
        }
    }
}