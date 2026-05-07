package dk.dtu.scout;

import dk.dtu.scout.crossover.Crossover;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.generator.Generator;
import dk.dtu.scout.logging.IterationSnapshot;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.SeriesMode;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.population.PopulationInitialization;
import dk.dtu.scout.population.PopulationModel;
import dk.dtu.scout.population.PopulationModelContext;
import dk.dtu.scout.population.PopulationState;
import dk.dtu.scout.population.PopulationStepResult;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.searchSpace.SearchSpace;
import dk.dtu.scout.selection.SelectionRule;
import dk.dtu.scout.stopcondition.StopCondition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationRunnerTest {

    @Test
    void run_initializesOptionalCrossoverWhenProvided() {
        TestPopulationModel populationModel = new TestPopulationModel(0);
        InitCountingCrossover crossover = new InitCountingCrossover();

        new SimulationRunner().run(
            populationModel,
            TestGenerator::new,
            crossover,
            null,
            new TestSelectionRule(),
            new TestSearchSpace(),
            new TestProblem(),
            new Random(1234L),
            List.of(new StopAtIteration(0)),
            List.of(),
            1
        );

        assertEquals(1, crossover.initCalls);
    }

    @Test
    void run_logsInitialAndTerminalStateAndNotifiesObservers() {
        TestPopulationModel populationModel = new TestPopulationModel(2);
        RecordingObserver observer = new RecordingObserver();
        StopAtIteration stopCondition = new StopAtIteration(2);

        RunLog log = run(populationModel, List.of(stopCondition), List.of(observer), 2);

        assertEquals(List.of(0, 2), log.getEvaluations());
        assertEquals(List.of("start-0", "step-0", "step-2", "end-2"), observer.events);
        assertEquals(List.of(0.0, 2.0), log.getSeries().get("fitness").getValues());
    }

    @Test
    void run_logsOnlyAtConfiguredIntervalAndAlwaysLogsTerminalState() {
        TestPopulationModel populationModel = new TestPopulationModel(3);
        RecordingObserver observer = new RecordingObserver();
        StopAtIteration stopCondition = new StopAtIteration(3);

        RunLog log = run(populationModel, List.of(stopCondition), List.of(observer), 2);

        assertEquals(List.of(0, 2, 3), log.getEvaluations());
        assertEquals(List.of("start-0", "step-0", "step-2", "step-3", "end-3"), observer.events);
        assertEquals(List.of(0.0, 2.0, 3.0), log.getSeries().get("fitness").getValues());
    }

    @Test
    void run_stopsImmediatelyWhenStopConditionAlreadyHolds() {
        TestPopulationModel populationModel = new TestPopulationModel(10);
        RecordingObserver observer = new RecordingObserver();
        StopAtIteration stopCondition = new StopAtIteration(0);

        RunLog log = run(populationModel, List.of(stopCondition), List.of(observer), 1);

        assertEquals(List.of(0), log.getEvaluations());
        assertEquals(0, populationModel.stepCalls);
        assertEquals(List.of("start-0", "step-0", "end-0"), observer.events);
    }

    @Test
    void run_initializesSharedStateWithCoreValues() {
        TestPopulationModel populationModel = new TestPopulationModel(1);
        StateReadingObserver observer = new StateReadingObserver();

        run(populationModel, List.of(new StopAtIteration(1)), List.of(observer), 1);

        assertTrue(observer.seenProblem instanceof TestProblem);
        assertEquals(5, observer.seenDimension);
        assertEquals("test-space", observer.seenSearchSpaceId);
    }

    @Test
    void run_mergesSharedStateFromPopulationInitializationAndSteps() {
        TestPopulationModel populationModel = new TestPopulationModel(2);
        StatePublishingObserver observer = new StatePublishingObserver();

        run(populationModel, List.of(new StopAtIteration(2)), List.of(observer), 1);

        assertTrue(observer.seenValues.contains("init"));
        assertTrue(observer.seenValues.contains("step-1"));
        assertTrue(observer.seenValues.contains("step-2"));
    }

    @Test
    void run_throwsCancellationExceptionWhenThreadIsInterrupted() {
        TestPopulationModel populationModel = new TestPopulationModel(10);

        Thread.currentThread().interrupt();

        try {
            assertThrows(CancellationException.class, () -> run(populationModel, List.of(new StopAtIteration(10)), List.of(), 1));
        } finally {
            Thread.interrupted();
        }
    }

    private static RunLog run(
        TestPopulationModel populationModel,
        List<StopCondition<Integer>> stopConditions,
        List<Observer<Integer>> observers,
        int logEveryEvaluations
    ) {
        return new SimulationRunner().run(
            populationModel,
            TestGenerator::new,
            null,
            null,
            new TestSelectionRule(),
            new TestSearchSpace(),
            new TestProblem(),
            new Random(1234L),
            stopConditions,
            observers,
            logEveryEvaluations
        );
    }

    private static IterationSnapshot<Integer> snapshot(int iteration) {
        EvaluatedSolution<Integer> solution = new EvaluatedSolution<>(iteration, iteration);
        return new IterationSnapshot<>(iteration, iteration + 1, solution, solution, true);
    }

    private static class TestPopulationModel implements PopulationModel<Integer> {
        private final int maxIteration;
        private int stepCalls;

        private TestPopulationModel(int maxIteration) {
            this.maxIteration = maxIteration;
        }

        @Override
        public PopulationInitialization<Integer> initialize(PopulationModelContext<Integer> context) {
            return new PopulationInitialization<>(
                new TestPopulationState(),
                snapshot(0),
                1,
                Map.of("phase", "init"),
                List.of()
            );
        }

        @Override
        public PopulationStepResult<Integer> step(
            PopulationModelContext<Integer> context,
            PopulationState<Integer> state,
            int iteration,
            int evaluations
        ) {
            stepCalls++;

            int nextIteration = iteration + 1;
            int boundedIteration = Math.min(nextIteration, maxIteration);

            return new PopulationStepResult<>(snapshot(boundedIteration), 1, Map.of("phase", "step-" + boundedIteration));
        }

        @Override
        public String id() {
            return "test-population";
        }

        @Override
        public String displayName() {
            return "Test population";
        }

        @Override
        public String description() {
            return "Test population model";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    private record TestPopulationState() implements PopulationState<Integer> {
    }

    private static class TestSearchSpace implements SearchSpace<Integer> {
        @Override
        public Integer randomSolution(Random rng) {
            return 0;
        }

        @Override
        public int dimension() {
            return 5;
        }

        @Override
        public String id() {
            return "test-space";
        }

        @Override
        public String displayName() {
            return "Test search space";
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

    private static class TestProblem implements Problem<Integer> {
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
            return "test-problem";
        }

        @Override
        public String displayName() {
            return "Test problem";
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

    private static class TestGenerator implements Generator<Integer> {
        @Override
        public Integer generate(Random rng) {
            return 0;
        }

        @Override
        public String id() {
            return "test-generator";
        }

        @Override
        public String displayName() {
            return "Test generator";
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

    private static class TestSelectionRule implements SelectionRule<Integer> {
        @Override
        public List<EvaluatedSolution<Integer>> select(
            List<EvaluatedSolution<Integer>> parents,
            List<EvaluatedSolution<Integer>> children,
            int mu,
            int iteration,
            Random rng
        ) {
            return parents;
        }

        @Override
        public String id() {
            return "test-selection";
        }

        @Override
        public String displayName() {
            return "Test selection";
        }

        @Override
        public String description() {
            return "Test selection";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    private static class StopAtIteration implements StopCondition<Integer> {
        private final int stopIteration;

        private StopAtIteration(int stopIteration) {
            this.stopIteration = stopIteration;
        }

        @Override
        public boolean shouldStop(int iteration, int evaluations, double bestFitness, Integer bestSolution) {
            return iteration >= stopIteration;
        }

        @Override
        public String id() {
            return "stop-at-iteration";
        }

        @Override
        public String displayName() {
            return "Stop at iteration";
        }

        @Override
        public String description() {
            return "Stops at a fixed iteration";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    private static class RecordingObserver implements Observer<Integer> {
        private final List<String> events = new ArrayList<>();

        @Override
        public void onStart(IterationSnapshot<Integer> state, RunLog log) {
            events.add("start-" + state.iteration());
        }

        @Override
        public void onStep(IterationSnapshot<Integer> state, RunLog log) {
            events.add("step-" + state.iteration());
            log.putSeries("fitness", state.bestFitness(), SeriesMode.ALL);
        }

        @Override
        public void onEnd(IterationSnapshot<Integer> state, RunLog log) {
            events.add("end-" + state.iteration());
        }

        @Override
        public String id() {
            return "recording-observer";
        }

        @Override
        public String displayName() {
            return "Recording observer";
        }

        @Override
        public String description() {
            return "Records observer calls";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    private static class StateReadingObserver implements Observer<Integer> {
        private Object seenProblem;
        private Object seenDimension;
        private Object seenSearchSpaceId;

        @Override
        public void init(State state) {
            seenProblem = state.get(StateKeys.PROBLEM);
            seenDimension = state.get(StateKeys.DIMENSION);
            seenSearchSpaceId = state.get(StateKeys.SEARCH_SPACE_ID);
        }

        @Override
        public void onStep(IterationSnapshot<Integer> state, RunLog log) {
        }

        @Override
        public String id() {
            return "state-reading-observer";
        }

        @Override
        public String displayName() {
            return "State reading observer";
        }

        @Override
        public String description() {
            return "Reads shared state";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    private static class StatePublishingObserver implements Observer<Integer> {
        private final List<Object> seenValues = new ArrayList<>();

        @Override
        public Map<String, Object> getStateVariables(State state) {
            Object phase = state.get("phase");

            if (phase != null) {
                seenValues.add(phase);
            }

            return Map.of();
        }

        @Override
        public void onStep(IterationSnapshot<Integer> state, RunLog log) {
        }

        @Override
        public String id() {
            return "state-publishing-observer";
        }

        @Override
        public String displayName() {
            return "State publishing observer";
        }

        @Override
        public String description() {
            return "Reads shared state values";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    private static class InitCountingCrossover implements Crossover<Integer> {
        private int initCalls;

        @Override
        public void init(State state) {
            initCalls++;
        }

        @Override
        public Integer crossover(Random rng) {
            return 0;
        }

        @Override
        public String id() {
            return "test-crossover";
        }

        @Override
        public String displayName() {
            return "Test crossover";
        }

        @Override
        public String description() {
            return "Counts init calls";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }
}