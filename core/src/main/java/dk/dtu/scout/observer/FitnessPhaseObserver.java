package dk.dtu.scout.observer;

import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.logging.IterationSnapshot;
import dk.dtu.scout.logging.RunLog;
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
    private final Deque<Integer> evaluationBlock = new ArrayDeque<>();

    private Integer lastIntervalEndEvaluation;

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
                new Parameter("windowSize", "Window Size (k)", "int", windowSize, 1.0, null, null),
                new Parameter("epsilon", "Phase Threshold (epsilon)", "double", epsilon, 0.0, null, null)
        );
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params.containsKey("windowSize")) {
            int value = ((Number) params.get("windowSize")).intValue();
            if (value <= 0) {
                throw new IllegalArgumentException("windowSize must be positive");
            }
            windowSize = value;
        }

        if (params.containsKey("epsilon")) {
            double value = ((Number) params.get("epsilon")).doubleValue();
            if (value < 0.0) {
                throw new IllegalArgumentException("epsilon must be non-negative");
            }
            epsilon = value;
        }
    }

    @Override
    public void onStart(IterationSnapshot<S> state, RunLog log) {
        fitnessBlock.clear();
        evaluationBlock.clear();
        lastIntervalEndEvaluation = null;
    }

    @Override
    public void onStep(IterationSnapshot<S> state, RunLog log) {
        fitnessBlock.addLast(state.currentFitness());
        evaluationBlock.addLast(Math.max(0, state.evaluations() - 1));

        if (fitnessBlock.size() < windowSize) {
            return;
        }

        emitCurrentBlock(log);

        fitnessBlock.clear();
        evaluationBlock.clear();
    }

    @Override
    public void onEnd(IterationSnapshot<S> state, RunLog log) {
        if (!fitnessBlock.isEmpty()) {
            emitCurrentBlock(log);
        }
    }

    private void emitCurrentBlock(RunLog log) {
        double delta = fitnessBlock.getLast() - fitnessBlock.getFirst();
        Phase phase = classify(delta);

        int startEvaluation = evaluationBlock.getFirst();
        int endEvaluation = evaluationBlock.getLast();

        if (lastIntervalEndEvaluation != null) {
            startEvaluation = lastIntervalEndEvaluation;
        }

        lastIntervalEndEvaluation = endEvaluation;

        log.putSeries(
            "fitnessPhaseIntervals",
            newInterval(startEvaluation, endEvaluation, phase),
            SeriesMode.ALL
        );
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

    private Map<String, Object> newInterval(int startEvaluation, int endEvaluation, Phase phase) {
        Map<String, Object> interval = new LinkedHashMap<>();
        interval.put("startEvaluation", startEvaluation);
        interval.put("endEvaluation", endEvaluation);
        interval.put("phase", phase.name());
        return interval;
    }
}