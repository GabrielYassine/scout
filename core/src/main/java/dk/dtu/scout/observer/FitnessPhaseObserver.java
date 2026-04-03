package dk.dtu.scout.observer;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;
import dk.dtu.scout.logging.SeriesMode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class FitnessPhaseObserver<S> implements Observer<S> {

    private static final int DEFAULT_WINDOW_SIZE = 10;
    private static final double DEFAULT_EPSILON = 1e-6;

    private int windowSize = DEFAULT_WINDOW_SIZE;
    private double epsilon = DEFAULT_EPSILON;

    private final Deque<Double> fitnessBlock = new ArrayDeque<>();
    private final Deque<Integer> iterationBlock = new ArrayDeque<>();
    private final Deque<Integer> evaluationBlock = new ArrayDeque<>();

    private enum Phase {
        IMPROVING,
        WORSENING,
        STAGNANT
    }

    @Override
    public String id() {
        return "fitness-phase";
    }

    @Override
    public String displayName() {
        return "Fitness Phase Observer";
    }

    @Override
    public String description() {
        return "Classifies fitness phases in completed non-overlapping blocks of fitness values";
    }

    @Override
    public List<Parameter> params() {
        return List.of(
                new Parameter("windowSize", "Window Size (k)", "int", windowSize, 1.0, null),
                new Parameter("epsilon", "Phase Threshold (epsilon)", "double", epsilon, 0.0, null)
        );
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) return;

        if (params.containsKey("windowSize")) {
            int value = ((Number) params.get("windowSize")).intValue();
            if (value <= 0) {
                throw new IllegalArgumentException("windowSize must be positive");
            }
            this.windowSize = value;
        }

        if (params.containsKey("epsilon")) {
            double value = ((Number) params.get("epsilon")).doubleValue();
            if (value < 0.0) {
                throw new IllegalArgumentException("epsilon must be non-negative");
            }
            this.epsilon = value;
        }
    }

    @Override
    public void onStart(RunState<S> state, RunLog log) {
        fitnessBlock.clear();
        iterationBlock.clear();
        evaluationBlock.clear();
    }

    @Override
    public void onStep(RunState<S> state, RunLog log) {
        double fitness = state.currentFitness();
        int iteration = state.iteration();
        int evaluation = Math.max(0, state.evaluations() - 1);

        fitnessBlock.addLast(fitness);
        iterationBlock.addLast(iteration);
        evaluationBlock.addLast(evaluation);

        // Wait until the whole block is complete before classifying/coloring it
        if (fitnessBlock.size() < windowSize) {
            return;
        }

        double firstFitness = fitnessBlock.getFirst();
        double lastFitness = fitnessBlock.getLast();
        Phase phase = classify(lastFitness - firstFitness);

        int startIteration = iterationBlock.getFirst();
        int endIteration = iterationBlock.getLast();
        int startEvaluation = evaluationBlock.getFirst();
        int endEvaluation = evaluationBlock.getLast();

        Map<String, Object> interval = newInterval(
                startIteration,
                endIteration,
                startEvaluation,
                endEvaluation,
                phase
        );

        emitInterval(log, interval);

        // Start a fresh new block after emitting this one
        fitnessBlock.clear();
        iterationBlock.clear();
        evaluationBlock.clear();
    }

    @Override
    public void onEnd(RunState<S> state, RunLog log) {
        // even if the last block is not full, we can still classify and emit it
        if (fitnessBlock.isEmpty()) {
            return;
        }

        double firstFitness = fitnessBlock.getFirst();
        double lastFitness = fitnessBlock.getLast();
        Phase phase = classify(lastFitness - firstFitness);

        int startIteration = iterationBlock.getFirst();
        int endIteration = iterationBlock.getLast();
        int startEvaluation = evaluationBlock.getFirst();
        int endEvaluation = evaluationBlock.getLast();

        Map<String, Object> interval = newInterval(
            startIteration,
            endIteration,
            startEvaluation,
            endEvaluation,
            phase
        );

        emitInterval(log, interval);
    }

    private Phase classify(double delta) {
        if (delta > epsilon) {
            return Phase.IMPROVING;
        }
        if (delta < -epsilon) {
            return Phase.WORSENING;
        }
        return Phase.STAGNANT;
    }

    private Map<String, Object> newInterval(
        int startIteration,
        int endIteration,
        int startEvaluation,
        int endEvaluation,
        Phase phase
    ) {
        Map<String, Object> interval = new LinkedHashMap<>();
        interval.put("startIteration", startIteration);
        interval.put("endIteration", endIteration);
        interval.put("startEvaluation", startEvaluation);
        interval.put("endEvaluation", endEvaluation);
        interval.put("phase", phase.name());
        return interval;
    }

    private void emitInterval(RunLog log, Map<String, Object> interval) {
        if (interval == null) return;
        log.putSeries("fitnessPhaseIntervals", new LinkedHashMap<>(interval), SeriesMode.ALL);
    }
}