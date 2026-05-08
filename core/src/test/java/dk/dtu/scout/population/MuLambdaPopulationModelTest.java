package dk.dtu.scout.population;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.dto.EvaluatedSolution;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static dk.dtu.scout.population.PopulationModelTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class MuLambdaPopulationModelTest {

    @Test
    void initialize_createsMuInitialParentsAndTracksBestRepresentative() {
        MuLambdaPopulationModel<String> model = new MuLambdaPopulationModel<>();

        model.configure(Map.of("mu", 2, "lambda", 1));

        TestSearchSpace space = new TestSearchSpace(queue("a", "best"));
        TestProblem problem = new TestProblem(Map.of("a", 1.0, "best", 5.0));
        TestGenerator generator = new TestGenerator(queue("unused"));
        State sharedState = new State();

        PopulationInitialization<String> initialization = model.initialize(stringContext(
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

        PopulationModelContext<String> context = stringContext(
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

        PopulationModelContext<String> context = stringContext(
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

        PopulationModelContext<String> context = stringContext(
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

        PopulationModelContext<String> context = stringContext(
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

        PopulationModelContext<String> context = stringContext(
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

        PopulationModelContext<String> context = stringContext(
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
}