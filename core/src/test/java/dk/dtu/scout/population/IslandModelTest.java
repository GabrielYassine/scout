package dk.dtu.scout.population;

import dk.dtu.scout.ScoutComponent;
import dk.dtu.scout.State;
import dk.dtu.scout.crossover.Crossover;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.generator.Generator;
import dk.dtu.scout.parentSelectionRule.ParentSelectionRule;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.searchSpace.SearchSpace;
import dk.dtu.scout.selection.SelectionRule;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

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

        PopulationInitialization<Integer> initialization = model.initialize(context(
            model,
            new SequentialSearchSpace(1),
            new IdentityProblem(),
            new BestSelectionRule<>(),
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

        PopulationModelContext<Integer> context = context(
            model,
            new SequentialSearchSpace(1),
            new IdentityProblem(),
            new BestSelectionRule<>(),
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

        PopulationModelContext<Integer> context = context(
            model,
            new SequentialSearchSpace(1),
            new IdentityProblem(),
            new BestSelectionRule<>(),
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
    void step_rejectsParentSelectionReturningTooFewParents() {
        IslandModel<Integer> model = new IslandModel<>();

        model.configure(Map.of(
            "numIslands", 1,
            "mu", 1,
            "lambda", 1
        ));

        PopulationModelContext<Integer> context = context(
            model,
            new SequentialSearchSpace(1),
            new IdentityProblem(),
            new BestSelectionRule<>(),
            null,
            new TooFewParentsSelection<>()
        );

        PopulationInitialization<Integer> initialization = model.initialize(context);

        assertThrows(IllegalStateException.class, () ->
            model.step(context, initialization.state(), 0, initialization.evaluations())
        );
    }

    @Test
    void step_rejectsSelectionReturningNoParents() {
        IslandModel<Integer> model = new IslandModel<>();

        model.configure(Map.of(
            "numIslands", 1,
            "mu", 1,
            "lambda", 1
        ));

        PopulationModelContext<Integer> context = context(
            model,
            new SequentialSearchSpace(1),
            new IdentityProblem(),
            new EmptySelectionRule<>(),
            null
        );

        PopulationInitialization<Integer> initialization = model.initialize(context);

        assertThrows(IllegalStateException.class, () ->
            model.step(context, initialization.state(), 0, initialization.evaluations())
        );
    }

    @Test
    void step_rejectsSelectionReturningWrongParentCount() {
        IslandModel<Integer> model = new IslandModel<>();

        model.configure(Map.of(
            "numIslands", 1,
            "mu", 2,
            "lambda", 2
        ));

        PopulationModelContext<Integer> context = context(
            model,
            new SequentialSearchSpace(1),
            new IdentityProblem(),
            new WrongCountSelectionRule<>(),
            null
        );

        PopulationInitialization<Integer> initialization = model.initialize(context);

        assertThrows(IllegalStateException.class, () ->
            model.step(context, initialization.state(), 0, initialization.evaluations())
        );
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

        PopulationModelContext<Integer> context = context(
                model,
                new FixedValuesSearchSpace(1, 2, 100, 101),
                new IdentityProblem(),
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
    void initialize_keepsFirstParentAsRepresentativeWhenSecondParentIsWorse() {
        IslandModel<Integer> model = new IslandModel<>();

        model.configure(Map.of(
                "numIslands", 1,
                "mu", 2,
                "lambda", 1
        ));

        PopulationInitialization<Integer> initialization = model.initialize(context(
                model,
                new FixedValuesSearchSpace(10, 1),
                new IdentityProblem(),
                new BestSelectionRule<>(),
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

        PopulationModelContext<Integer> context = context(
                model,
                new FixedValuesSearchSpace(1, 100),
                new IdentityProblem(),
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
    void configure_ignoresNullAndMissingParams() {
        IslandModel<Integer> model = new IslandModel<>();

        assertDoesNotThrow(() -> model.configure(Map.of()));
        assertDoesNotThrow(() -> model.configure(Map.of()));
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
    void step_rejectsSelectionReturningNull() {
        IslandModel<Integer> model = new IslandModel<>();

        model.configure(Map.of(
                "numIslands", 1,
                "mu", 1,
                "lambda", 1
        ));

        PopulationModelContext<Integer> context = context(
                model,
                new SequentialSearchSpace(1),
                new IdentityProblem(),
                new NullSelectionRule<>(),
                null
        );

        PopulationInitialization<Integer> initialization = model.initialize(context);

        assertThrows(IllegalStateException.class, () ->
                model.step(context, initialization.state(), 0, initialization.evaluations())
        );
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

        PopulationModelContext<Integer> context = context(
                model,
                new FixedValuesSearchSpace(10, 1, 100, 50),
                new IdentityProblem(),
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

    @Test
    void step_rejectsParentSelectionReturningNull() {
        IslandModel<Integer> model = new IslandModel<>();

        model.configure(Map.of(
                "numIslands", 1,
                "mu", 1,
                "lambda", 1
        ));

        PopulationModelContext<Integer> context = context(
                model,
                new SequentialSearchSpace(1),
                new IdentityProblem(),
                new BestSelectionRule<>(),
                null,
                new NullParentSelection<>()
        );

        PopulationInitialization<Integer> initialization = model.initialize(context);

        assertThrows(IllegalStateException.class, () ->
                model.step(context, initialization.state(), 0, initialization.evaluations())
        );
    }


    private static class NullParentSelection<S> extends DuplicateParentSelection<S> {
        @Override
        public List<EvaluatedSolution<S>> selectParents(List<EvaluatedSolution<S>> parents, Random rng) {
            return null;
        }
    }

    private static PopulationModelContext<Integer> context(
        IslandModel<Integer> model,
        SearchSpace<Integer> space,
        Problem<Integer> problem,
        SelectionRule<Integer> selection,
        Crossover<Integer> crossover
    ) {
        return context(model, space, problem, selection, crossover, new DuplicateParentSelection<>());
    }

    private static PopulationModelContext<Integer> context(
        IslandModel<Integer> model,
        SearchSpace<Integer> space,
        Problem<Integer> problem,
        SelectionRule<Integer> selection,
        Crossover<Integer> crossover,
        ParentSelectionRule<Integer> parentSelection
    ) {
        State sharedState = new State();
        sharedState.update(Map.of(
            StateKeys.PROBLEM, problem,
            StateKeys.DIMENSION, space.dimension(),
            StateKeys.SEARCH_SPACE_ID, space.id()
        ));

        Supplier<Generator<Integer>> generatorFactory = IncrementGenerator::new;

        return new PopulationModelContext<>(
            generatorFactory,
            parentSelection,
            crossover,
            selection,
            space,
            problem,
            new Random(1234L),
            sharedState
        );
    }

    private static class SequentialSearchSpace implements SearchSpace<Integer> {
        private int next;

        private SequentialSearchSpace(int start) {
            this.next = start;
        }

        @Override
        public Integer randomSolution(Random rng) {
            return next++;
        }

        @Override
        public int dimension() {
            return 10;
        }

        @Override
        public String id() {
            return "integer-space";
        }

        @Override
        public String displayName() {
            return "Integer space";
        }

        @Override
        public String description() {
            return "Integer test space";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    private static class FixedValuesSearchSpace implements SearchSpace<Integer> {
        private final int[] values;
        private int index;

        private FixedValuesSearchSpace(int... values) {
            this.values = values;
        }

        @Override
        public Integer randomSolution(Random rng) {
            return values[index++ % values.length];
        }

        @Override
        public int dimension() {
            return 10;
        }

        @Override
        public String id() {
            return "fixed-values";
        }

        @Override
        public String displayName() {
            return "Fixed values";
        }

        @Override
        public String description() {
            return "Fixed test values";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    private static class IdentityProblem implements Problem<Integer> {
        @Override
        public double fitness(Integer solution) {
            return solution;
        }

        @Override
        public boolean isOptimal(double fitness) {
            return false;
        }

        @Override
        public String id() {
            return "identity";
        }

        @Override
        public String displayName() {
            return "Identity";
        }

        @Override
        public String description() {
            return "Fitness equals value";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    private static class IncrementGenerator implements Generator<Integer> {
        private State state;

        @Override
        public void init(State state) {
            this.state = state;
        }

        @Override
        public Integer generate(Random rng) {
            return ((Number) state.get(StateKeys.OFFSPRING_BASE)).intValue() + 10;
        }

        @Override
        public Map<String, Object> getStateVariables(State state) {
            return Map.of("generatorSeenCurrent", state.get(StateKeys.CURRENT));
        }

        @Override
        public String id() {
            return "increment-generator";
        }

        @Override
        public String displayName() {
            return "Increment generator";
        }

        @Override
        public String description() {
            return "Adds ten";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    private static class FixedCrossover implements Crossover<Integer> {
        private final int value;
        private int calls;

        private FixedCrossover(int value) {
            this.value = value;
        }

        @Override
        public Integer crossover(Random rng) {
            calls++;
            return value;
        }

        @Override
        public String id() {
            return "fixed-crossover";
        }

        @Override
        public String displayName() {
            return "Fixed crossover";
        }

        @Override
        public String description() {
            return "Returns fixed value";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    private static class DuplicateParentSelection<S> implements ParentSelectionRule<S> {
        @Override
        public List<EvaluatedSolution<S>> selectParents(List<EvaluatedSolution<S>> parents, Random rng) {
            return List.of(parents.getFirst(), parents.getFirst());
        }

        @Override
        public String id() {
            return "duplicate-parent-selection";
        }

        @Override
        public String displayName() {
            return "Duplicate parent selection";
        }

        @Override
        public String description() {
            return "Duplicates first parent";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    private static class TooFewParentsSelection<S> extends DuplicateParentSelection<S> {
        @Override
        public List<EvaluatedSolution<S>> selectParents(List<EvaluatedSolution<S>> parents, Random rng) {
            return List.of(parents.getFirst());
        }
    }

    private static class BestSelectionRule<S> implements SelectionRule<S> {
        @Override
        public List<EvaluatedSolution<S>> select(
            List<EvaluatedSolution<S>> parents,
            List<EvaluatedSolution<S>> children,
            int mu,
            int iteration,
            Random rng
        ) {
            List<EvaluatedSolution<S>> combined = new ArrayList<>();
            combined.addAll(parents);
            combined.addAll(children);
            combined.sort(Comparator.comparingDouble(EvaluatedSolution<S>::fitness).reversed());
            return new ArrayList<>(combined.subList(0, mu));
        }

        @Override
        public String id() {
            return "best-selection";
        }

        @Override
        public String displayName() {
            return "Best selection";
        }

        @Override
        public String description() {
            return "Selects best values";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    private static class KeepParentsSelectionRule<S> implements SelectionRule<S> {
        @Override
        public List<EvaluatedSolution<S>> select(
            List<EvaluatedSolution<S>> parents,
            List<EvaluatedSolution<S>> children,
            int mu,
            int iteration,
            Random rng
        ) {
            return new ArrayList<>(parents.subList(0, mu));
        }

        @Override
        public String id() {
            return "keep-parents";
        }

        @Override
        public String displayName() {
            return "Keep parents";
        }

        @Override
        public String description() {
            return "Keeps parents";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    private static class EmptySelectionRule<S> extends BestSelectionRule<S> {
        @Override
        public List<EvaluatedSolution<S>> select(
            List<EvaluatedSolution<S>> parents,
            List<EvaluatedSolution<S>> children,
            int mu,
            int iteration,
            Random rng
        ) {
            return List.of();
        }
    }

    private static class WrongCountSelectionRule<S> extends BestSelectionRule<S> {
        @Override
        public List<EvaluatedSolution<S>> select(
            List<EvaluatedSolution<S>> parents,
            List<EvaluatedSolution<S>> children,
            int mu,
            int iteration,
            Random rng
        ) {
            List<EvaluatedSolution<S>> combined = new ArrayList<>();
            combined.addAll(parents);
            combined.addAll(children);
            return List.of(combined.getFirst());
        }
    }

    private static class NullSelectionRule<S> extends BestSelectionRule<S> {
        @Override
        public List<EvaluatedSolution<S>> select(
                List<EvaluatedSolution<S>> parents,
                List<EvaluatedSolution<S>> children,
                int mu,
                int iteration,
                Random rng
        ) {
            return null;
        }
    }
}