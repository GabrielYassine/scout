package dk.dtu.scout.population;

import dk.dtu.scout.datatypes.StateKeys;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static dk.dtu.scout.population.PopulationModelTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class IslandModelTest {

    @Test
    void initialize_createsConfiguredIslandsAndInitialSnapshot() {
        IslandModel<Integer> model = new IslandModel<>();

        model.configure(Map.of(
            "numIslands", 2,
            "mu", 2,
            "lambda", 1,
            "epochLength", 10
        ));

        PopulationInitialization<Integer> initialization = model.initialize(integerContext(
            new SequentialSearchSpace(1),
            new IdentityProblem(),
            IncrementGenerator::new,
            new DuplicateParentSelection<>(),
            new KeepBestSelection<>(),
            null
        ));

        assertNotNull(initialization.state());
        assertEquals(4, initialization.evaluations());
        assertEquals(4, initialization.initialState().evaluations());
        assertEquals(4, initialization.initialState().currentFitness());
        assertEquals(4, initialization.initialState().bestFitness());

        assertEquals(4, initialization.sharedStateVariables().get(StateKeys.CURRENT));
        assertEquals(4, initialization.sharedStateVariables().get(StateKeys.BEST));
        assertTrue(initialization.stateComponents().isEmpty());
    }

    @Test
    void step_generatesOneChildPerIslandAndUpdatesBest() {
        IslandModel<Integer> model = new IslandModel<>();

        model.configure(Map.of(
            "numIslands", 2,
            "mu", 1,
            "lambda", 1,
            "epochLength", 10
        ));

        PopulationModelContext<Integer> context = integerContext(
            new SequentialSearchSpace(1),
            new IdentityProblem(),
            IncrementGenerator::new,
            new DuplicateParentSelection<>(),
            new KeepBestSelection<>(),
            null
        );

        PopulationInitialization<Integer> initialization = model.initialize(context);

        PopulationStepResult<Integer> result = model.step(
            context,
            initialization.state(),
            0,
            initialization.evaluations()
        );

        assertEquals(2, result.evaluationsDelta());
        assertEquals(4, result.runState().evaluations());
        assertEquals(12, result.runState().currentSolution());
        assertEquals(12.0, result.runState().currentFitness());
        assertEquals(12, result.runState().bestSolution());
        assertEquals(12.0, result.runState().bestFitness());
        assertTrue(result.runState().accepted());

        assertEquals(12, result.sharedStateVariables().get(StateKeys.CURRENT));
        assertEquals(12, result.sharedStateVariables().get(StateKeys.BEST));
        assertEquals(12.0, result.sharedStateVariables().get(StateKeys.BEST_FITNESS));
    }

    @Test
    void step_usesCrossoverWhenProvided() {
        IslandModel<Integer> model = new IslandModel<>();

        model.configure(Map.of(
            "numIslands", 1,
            "mu", 1,
            "lambda", 1,
            "epochLength", 10
        ));

        FixedCrossover crossover = new FixedCrossover(100);

        PopulationModelContext<Integer> context = integerContext(
            new SequentialSearchSpace(1),
            new IdentityProblem(),
            IncrementGenerator::new,
            new DuplicateParentSelection<>(),
            new KeepBestSelection<>(),
            crossover
        );

        PopulationInitialization<Integer> initialization = model.initialize(context);

        PopulationStepResult<Integer> result = model.step(
            context,
            initialization.state(),
            0,
            initialization.evaluations()
        );

        assertEquals(1, crossover.calls);
        assertEquals(110, result.runState().currentSolution());
    }

    @Test
    void migrationCanReplaceWorstParentWhenIslandHasMultipleParents() {
        IslandModel<Integer> model = new IslandModel<>();

        model.configure(Map.of(
            "numIslands", 2,
            "mu", 2,
            "lambda", 1,
            "epochLength", 1
        ));

        PopulationModelContext<Integer> context = integerContext(
            new FixedValuesSearchSpace(1, 2, 100, 101),
            new IdentityProblem(),
            IncrementGenerator::new,
            new DuplicateParentSelection<>(),
            new KeepParentsSelectionRule<>(),
            null
        );

        PopulationInitialization<Integer> initialization = model.initialize(context);

        PopulationStepResult<Integer> result = model.step(
            context,
            initialization.state(),
            0,
            initialization.evaluations()
        );

        assertEquals(2, result.evaluationsDelta());
        assertEquals(101, result.runState().currentSolution());
        assertEquals(101.0, result.runState().currentFitness());
        assertEquals(101, result.runState().bestSolution());
        assertEquals(101.0, result.runState().bestFitness());
    }

    @Test
    void initialize_keepsInitialBestWhenLaterParentIsWorse() {
        IslandModel<Integer> model = new IslandModel<>();

        model.configure(Map.of(
            "numIslands", 1,
            "mu", 2,
            "lambda", 1
        ));

        PopulationInitialization<Integer> initialization = model.initialize(integerContext(
            new FixedValuesSearchSpace(10, 1),
            new IdentityProblem(),
            IncrementGenerator::new,
            new DuplicateParentSelection<>(),
            new KeepBestSelection<>(),
            null
        ));

        assertEquals(10, initialization.initialState().currentSolution());
        assertEquals(10.0, initialization.initialState().currentFitness());
    }

    @Test
    void step_runsMigrationAtEpochBoundary() {
        IslandModel<Integer> model = new IslandModel<>();

        model.configure(Map.of(
            "numIslands", 2,
            "mu", 1,
            "lambda", 1,
            "epochLength", 1
        ));

        PopulationModelContext<Integer> context = integerContext(
            new FixedValuesSearchSpace(1, 100),
            new IdentityProblem(),
            IncrementGenerator::new,
            new DuplicateParentSelection<>(),
            new KeepParentsSelectionRule<>(),
            null
        );

        PopulationInitialization<Integer> initialization = model.initialize(context);

        PopulationStepResult<Integer> result = model.step(
            context,
            initialization.state(),
            0,
            initialization.evaluations()
        );

        assertEquals(2, result.evaluationsDelta());
        assertEquals(100, result.runState().currentSolution());
        assertEquals(100.0, result.runState().currentFitness());
        assertEquals(100, result.runState().bestSolution());
        assertEquals(100.0, result.runState().bestFitness());
    }

    @Test
    void configure_rejectsInvalidValues() {
        IslandModel<Integer> model = new IslandModel<>();

        assertThrows(IllegalArgumentException.class, () -> model.configure(Map.of("numIslands", 0)));
        assertThrows(IllegalArgumentException.class, () -> model.configure(Map.of("mu", 0)));
        assertThrows(IllegalArgumentException.class, () -> model.configure(Map.of("lambda", 0)));
        assertThrows(IllegalArgumentException.class, () -> model.configure(Map.of("epochLength", 0)));
    }

    @Test
    void metadata_isStable() {
        IslandModel<Integer> model = new IslandModel<>();

        assertEquals("islands", model.id());
        assertEquals("Island Model", model.displayName());
        assertFalse(model.description().isBlank());
        assertEquals(4, model.params().size());
    }

    @Test
    void migrationFindsWorstParentBeyondFirstPosition() {
        IslandModel<Integer> model = new IslandModel<>();

        model.configure(Map.of(
            "numIslands", 2,
            "mu", 2,
            "lambda", 1,
            "epochLength", 1
        ));

        PopulationModelContext<Integer> context = integerContext(
            new FixedValuesSearchSpace(10, 1, 100, 50),
            new IdentityProblem(),
            IncrementGenerator::new,
            new DuplicateParentSelection<>(),
            new KeepParentsSelectionRule<>(),
            null
        );

        PopulationInitialization<Integer> initialization = model.initialize(context);

        PopulationStepResult<Integer> result = model.step(
            context,
            initialization.state(),
            0,
            initialization.evaluations()
        );

        assertEquals(2, result.evaluationsDelta());
        assertEquals(100, result.runState().bestSolution());
        assertEquals(100.0, result.runState().bestFitness());
    }
}