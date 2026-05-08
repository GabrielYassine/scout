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

/**
 * Observer that classifies fitness phases in completed non-overlapping blocks of fitness values.
 * @author s235257
 */

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
    /**
     * Collects fitness values and evaluation counts in blocks of size k, classifies the phase of each block,
     * and logs the intervals with their corresponding phases.
     * @param state current iteration snapshot
     * @param log run log where the fitness phase intervals are stored
     */
    @Override
    public void onStart(IterationSnapshot<S> state, RunLog log) {
        fitnessBlock.clear();
        evaluationBlock.clear();
        lastIntervalEndEvaluation = null;
    }
    /**
     * Adds the current fitness and evaluation count to the current block. Once the block is full, classifies
     * the phase and logs the interval before starting a new block.
     * @param state current iteration snapshot
     * @param log run log where the fitness phase intervals are stored
     */
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
    /**
     * If there are any remaining fitness values in the block at the end of the run, classifies the phase and logs the final interval.
     * @param state current iteration snapshot
     * @param log run log where the fitness phase intervals are stored
     */
    @Override
    public void onEnd(IterationSnapshot<S> state, RunLog log) {
        if (!fitnessBlock.isEmpty()) {
            emitCurrentBlock(log);
        }
    }
    /**
     * Classifies the current block of fitness values as improving, worsening, or stagnant based on the change from the first to the last value,
     * and logs the corresponding evaluation interval with its phase.
     * @param log run log where the fitness phase intervals are stored
     */
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
    /**
     * Classifies the fitness change as improving, worsening, or stagnant based on the specified epsilon threshold.
     * @param delta change in fitness from the first to the last value in the block
     * @return the classified phase of the fitness change
     */
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