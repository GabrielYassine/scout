package dk.dtu.scout.population;

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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class MuLambdaPopulationModelTest {

    @Test
    void initialize_createsMuInitialParentsAndTracksBestRepresentative() {
        MuLambdaPopulationModel<String> model = new MuLambdaPopulationModel<>();

        model.configure(Map.of("mu", 2, "lambda", 1));

        Queue<String> initialSolutions = queue("a", "best");
        TestSearchSpace space = new TestSearchSpace(initialSolutions);
        TestProblem problem = new TestProblem(Map.of("a", 1.0, "best", 5.0));
        TestGenerator generator = new TestGenerator(queue("unused"));
        State sharedState = new State();

        PopulationInitialization<String> initialization = model.initialize(context(
            space,
            problem,
            () -> generator,
            new FirstTwoParentSelection<>(),
            new KeepBestSelection<>(),
            null,
            sharedState
        ));

        assertEquals(2, initialization.evaluations());
        assertEquals(0, initialization.initialState().iteration());
        assertEquals(2, initialization.initialState().evaluations());
        assertEquals("best", initialization.initialState().currentSolution());
        assertEquals("best", initialization.initialState().bestSolution());
        assertEquals(5.0, initialization.initialState().currentFitness(), 1e-9);
        assertEquals(5.0, initialization.initialState().bestFitness(), 1e-9);
        assertTrue(initialization.initialState().accepted());

        assertSame(generator, initialization.stateComponents().getFirst());

        Map<String, Object> variables = initialization.sharedStateVariables();

        assertEquals("best", variables.get(StateKeys.CURRENT));
        assertEquals("best", variables.get(StateKeys.BEST));
        assertEquals(5.0, (double) variables.get(StateKeys.CURRENT_FITNESS), 1e-9);
        assertEquals(5.0, (double) variables.get(StateKeys.BEST_FITNESS), 1e-9);

        List<?> parents = (List<?>) variables.get(StateKeys.PARENTS_EVALUATED);
        List<?> generation = (List<?>) variables.get(StateKeys.GENERATION_EVALUATED);

        assertEquals(2, parents.size());
        assertTrue(generation.isEmpty());
    }

    @Test
    void step_generatesLambdaChildrenAndUsesSelectionForNextParents() {
        MuLambdaPopulationModel<String> model = new MuLambdaPopulationModel<>();

        model.configure(Map.of("mu", 2, "lambda", 2));

        TestSearchSpace space = new TestSearchSpace(queue("p1", "p2"));
        TestProblem problem = new TestProblem(Map.of(
            "p1", 1.0,
            "p2", 2.0,
            "c1", 3.0,
            "c2", 4.0
        ));
        TestGenerator generator = new TestGenerator(queue("c1", "c2"));
        State sharedState = new State();

        PopulationModelContext<String> context = context(
            space,
            problem,
            () -> generator,
            new FirstTwoParentSelection<>(),
            new KeepBestSelection<>(),
            null,
            sharedState
        );

        PopulationInitialization<String> initialization = model.initialize(context);

        PopulationStepResult<String> result = model.step(
            context,
            initialization.state(),
            1,
            initialization.evaluations()
        );

        assertEquals(2, result.evaluationsDelta());
        assertEquals(4, result.runState().evaluations());
        assertEquals(1, result.runState().iteration());

        assertEquals("c2", result.runState().currentSolution());
        assertEquals("c2", result.runState().bestSolution());
        assertEquals(4.0, result.runState().currentFitness(), 1e-9);
        assertEquals(4.0, result.runState().bestFitness(), 1e-9);
        assertTrue(result.runState().accepted());

        List<?> generation = (List<?>) result.sharedStateVariables().get(StateKeys.GENERATION_EVALUATED);
        List<?> parents = (List<?>) result.sharedStateVariables().get(StateKeys.PARENTS_EVALUATED);

        assertEquals(2, generation.size());
        assertEquals(2, parents.size());
    }

    @Test
    void step_setsSelectedParentsAndOffspringBaseWhenCrossoverExists() {
        MuLambdaPopulationModel<String> model = new MuLambdaPopulationModel<>();

        model.configure(Map.of("mu", 2, "lambda", 1));

        TestSearchSpace space = new TestSearchSpace(queue("p1", "p2"));
        TestProblem problem = new TestProblem(Map.of(
            "p1", 1.0,
            "p2", 2.0,
            "child", 3.0
        ));
        TestGenerator generator = new TestGenerator(queue("child"));
        TestCrossover crossover = new TestCrossover("crossed");
        State sharedState = new State();

        PopulationModelContext<String> context = context(
            space,
            problem,
            () -> generator,
            new FirstTwoParentSelection<>(),
            new KeepBestSelection<>(),
            crossover,
            sharedState
        );

        PopulationInitialization<String> initialization = model.initialize(context);

        model.step(context, initialization.state(), 1, initialization.evaluations());

        assertEquals("p1", sharedState.get(StateKeys.SELECTED_PARENT_1));
        assertEquals("p2", sharedState.get(StateKeys.SELECTED_PARENT_2));
        assertEquals("crossed", sharedState.get(StateKeys.OFFSPRING_BASE));
        assertTrue(crossover.wasCalled());
    }

    @Test
    void step_usesFirstParentAsOffspringBaseWhenCrossoverIsMissing() {
        MuLambdaPopulationModel<String> model = new MuLambdaPopulationModel<>();

        model.configure(Map.of("mu", 2, "lambda", 1));

        TestSearchSpace space = new TestSearchSpace(queue("p1", "p2"));
        TestProblem problem = new TestProblem(Map.of(
            "p1", 1.0,
            "p2", 2.0,
            "child", 3.0
        ));
        TestGenerator generator = new TestGenerator(queue("child"));
        State sharedState = new State();

        PopulationModelContext<String> context = context(
            space,
            problem,
            () -> generator,
            new FirstTwoParentSelection<>(),
            new KeepBestSelection<>(),
            null,
            sharedState
        );

        PopulationInitialization<String> initialization = model.initialize(context);

        model.step(context, initialization.state(), 1, initialization.evaluations());

        assertEquals("p1", sharedState.get(StateKeys.SELECTED_PARENT_1));
        assertEquals("p2", sharedState.get(StateKeys.SELECTED_PARENT_2));
        assertEquals("p1", sharedState.get(StateKeys.OFFSPRING_BASE));
    }

    @Test
    void step_acceptedIsFalseWhenRepresentativeObjectDoesNotChange() {
        MuLambdaPopulationModel<String> model = new MuLambdaPopulationModel<>();

        model.configure(Map.of("mu", 2, "lambda", 1));

        TestSearchSpace space = new TestSearchSpace(queue("p1", "p2"));
        TestProblem problem = new TestProblem(Map.of(
            "p1", 5.0,
            "p2", 1.0,
            "child", 0.0
        ));
        TestGenerator generator = new TestGenerator(queue("child"));
        State sharedState = new State();

        PopulationModelContext<String> context = context(
            space,
            problem,
            () -> generator,
            new FirstTwoParentSelection<>(),
            new KeepExistingParentsSelection<>(),
            null,
            sharedState
        );

        PopulationInitialization<String> initialization = model.initialize(context);

        PopulationStepResult<String> result = model.step(
            context,
            initialization.state(),
            1,
            initialization.evaluations()
        );

        assertEquals("p1", result.runState().currentSolution());
        assertFalse(result.runState().accepted());
    }

    @Test
    void step_keepsGlobalBestWhenSelectedPopulationWorsens() {
        MuLambdaPopulationModel<String> model = new MuLambdaPopulationModel<>();

        model.configure(Map.of("mu", 2, "lambda", 1));

        TestSearchSpace space = new TestSearchSpace(queue("best", "weak"));
        TestProblem problem = new TestProblem(Map.of(
            "best", 10.0,
            "weak", 1.0,
            "child", 0.0
        ));
        TestGenerator generator = new TestGenerator(queue("child"));
        State sharedState = new State();

        PopulationModelContext<String> context = context(
            space,
            problem,
            () -> generator,
            new FirstTwoParentSelection<>(),
            new SelectWeakParentsSelection<>(),
            null,
            sharedState
        );

        PopulationInitialization<String> initialization = model.initialize(context);

        PopulationStepResult<String> result = model.step(
            context,
            initialization.state(),
            1,
            initialization.evaluations()
        );

        assertEquals("weak", result.runState().currentSolution());
        assertEquals(1.0, result.runState().currentFitness(), 1e-9);

        assertEquals("best", result.runState().bestSolution());
        assertEquals(10.0, result.runState().bestFitness(), 1e-9);
    }

    @Test
    void configure_rejectsInvalidMuAndLambda() {
        MuLambdaPopulationModel<String> model = new MuLambdaPopulationModel<>();

        assertThrows(IllegalArgumentException.class, () -> model.configure(Map.of("mu", 0)));
        assertThrows(IllegalArgumentException.class, () -> model.configure(Map.of("mu", -1)));
        assertThrows(IllegalArgumentException.class, () -> model.configure(Map.of("lambda", 0)));
        assertThrows(IllegalArgumentException.class, () -> model.configure(Map.of("lambda", -1)));
    }

    @Test
    void step_representativeIsBestParentEvenWhenSelectionReturnsUnsortedParents() {
        MuLambdaPopulationModel<String> model = new MuLambdaPopulationModel<>();

        model.configure(Map.of("mu", 2, "lambda", 1));

        TestSearchSpace space = new TestSearchSpace(queue("p1", "p2"));
        TestProblem problem = new TestProblem(Map.of(
            "p1", 1.0,
            "p2", 2.0,
            "child", 0.0
        ));
        TestGenerator generator = new TestGenerator(queue("child"));
        State sharedState = new State();

        PopulationModelContext<String> context = context(
            space,
            problem,
            () -> generator,
            new FirstTwoParentSelection<>(),
            new KeepExistingParentsSelection<>(),
            null,
            sharedState
        );

        PopulationInitialization<String> initialization = model.initialize(context);

        PopulationStepResult<String> result = model.step(
            context,
            initialization.state(),
            1,
            initialization.evaluations()
        );

        assertEquals("p2", result.runState().currentSolution());
        assertEquals(2.0, result.runState().currentFitness(), 1e-9);
    }

    @Test
    void metadata_isStable() {
        MuLambdaPopulationModel<String> model = new MuLambdaPopulationModel<>();

        assertEquals("mu-lambda", model.id());
        assertEquals("Mu-Lambda Population Model", model.displayName());
        assertFalse(model.description().isBlank());
        assertEquals(2, model.params().size());
    }

    private static PopulationModelContext<String> context(
        SearchSpace<String> space,
        Problem<String> problem,
        Supplier<Generator<String>> generatorFactory,
        ParentSelectionRule<String> parentSelection,
        SelectionRule<String> selection,
        Crossover<String> crossover,
        State sharedState
    ) {
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

    private static Queue<String> queue(String... values) {
        return new ArrayDeque<>(List.of(values));
    }

    private record TestSearchSpace(Queue<String> solutions) implements SearchSpace<String> {

        @Override
        public String randomSolution(Random rng) {
            return solutions.remove();
        }

        @Override
        public int dimension() {
            return 1;
        }

        @Override
        public String id() {
            return "test-space";
        }

        @Override
        public String displayName() {
            return "Test Space";
        }

        @Override
        public String description() {
            return "Test search space";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    private record TestProblem(Map<String, Double> fitnessValues) implements Problem<String> {

        @Override
        public double fitness(String solution) {
            return fitnessValues.getOrDefault(solution, 0.0);
        }

        @Override
        public boolean isOptimal(double fitness) {
            return false;
        }

        @Override
        public String id() {
            return "test-problem";
        }

        @Override
        public String displayName() {
            return "Test Problem";
        }

        @Override
        public String description() {
            return "Test problem";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    private record TestGenerator(Queue<String> children) implements Generator<String> {

        @Override
        public String generate(Random rng) {
            return children.remove();
        }

        @Override
        public String id() {
            return "test-generator";
        }

        @Override
        public String displayName() {
            return "Test Generator";
        }

        @Override
        public String description() {
            return "Test generator";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    private static final class TestCrossover implements Crossover<String> {
        private final String child;
        private boolean called;

        private TestCrossover(String child) {
            this.child = child;
        }

        private boolean wasCalled() {
            return called;
        }

        @Override
        public String crossover(Random rng) {
            called = true;
            return child;
        }

        @Override
        public String id() {
            return "test-crossover";
        }

        @Override
        public String displayName() {
            return "Test Crossover";
        }

        @Override
        public String description() {
            return "Test crossover";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    private static class FirstTwoParentSelection<S> implements ParentSelectionRule<S> {
        @Override
        public List<EvaluatedSolution<S>> selectParents(List<EvaluatedSolution<S>> population, Random rng) {
            return List.of(population.get(0), population.get(1));
        }

        @Override
        public String id() {
            return "first-two";
        }

        @Override
        public String displayName() {
            return "First Two";
        }

        @Override
        public String description() {
            return "Selects first two parents";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    private static class KeepBestSelection<S> implements SelectionRule<S> {
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

            combined.sort((a, b) -> Double.compare(b.fitness(), a.fitness()));

            return new ArrayList<>(combined.subList(0, mu));
        }

        @Override
        public String id() {
            return "keep-best";
        }

        @Override
        public String displayName() {
            return "Keep Best";
        }

        @Override
        public String description() {
            return "Keeps best";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    private static final class KeepExistingParentsSelection<S> extends KeepBestSelection<S> {
        @Override
        public List<EvaluatedSolution<S>> select(
            List<EvaluatedSolution<S>> parents,
            List<EvaluatedSolution<S>> children,
            int mu,
            int iteration,
            Random rng
        ) {
            return parents;
        }
    }

    private static final class SelectWeakParentsSelection<S> extends KeepBestSelection<S> {
        @Override
        public List<EvaluatedSolution<S>> select(
            List<EvaluatedSolution<S>> parents,
            List<EvaluatedSolution<S>> children,
            int mu,
            int iteration,
            Random rng
        ) {
            return List.of(parents.get(1), children.getFirst());
        }
    }
}