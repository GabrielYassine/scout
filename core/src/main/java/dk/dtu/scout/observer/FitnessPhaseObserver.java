package dk.dtu.scout.observer;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;
import dk.dtu.scout.logging.SeriesMode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
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

    private final Deque<Double> window = new ArrayDeque<>();
    private final List<Map<String, Object>> intervals = new ArrayList<>();
    private Phase currentPhase = null;

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
        return "Classifies fitness phases using a rolling window of current fitness values";
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
        window.clear();
        intervals.clear();
        currentPhase = null;
    }

    @Override
    public void onStep(RunState<S> state, RunLog log) {
        double fitness = state.currentFitness();
        int iteration = state.iteration();

        window.addLast(fitness);
        if (window.size() > windowSize) {
            window.removeFirst();
        }

        if (window.size() < windowSize) {
            return;
        }

        double first = window.getFirst();
        double last = window.getLast();
        Phase detected = classify(last - first);

        int windowStart = iteration - windowSize + 1;

        if (currentPhase == null) {
            currentPhase = detected;
            intervals.add(newInterval(windowStart, iteration, currentPhase));
            log.putSeries("fitnessPhaseIntervals", new ArrayList<>(intervals), SeriesMode.LATEST_ONLY);
            return;
        }

        if (detected == currentPhase) {
            updateIntervalEnd(iteration);
            log.putSeries("fitnessPhaseIntervals", new ArrayList<>(intervals), SeriesMode.LATEST_ONLY);
            return;
        }

        currentPhase = detected;
        intervals.add(newInterval(windowStart, iteration, currentPhase));
        log.putSeries("fitnessPhaseIntervals", new ArrayList<>(intervals), SeriesMode.LATEST_ONLY);
    }

    @Override
    public void onEnd(RunState<S> state, RunLog log) {
        if (!intervals.isEmpty()) {
            updateIntervalEnd(state.iteration());
            log.putSeries("fitnessPhaseIntervals", new ArrayList<>(intervals), SeriesMode.LATEST_ONLY);
        }
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

    private Map<String, Object> newInterval(int start, int end, Phase phase) {
        Map<String, Object> interval = new LinkedHashMap<>();
        interval.put("startIteration", start);
        interval.put("endIteration", end);
        interval.put("phase", phase.name());
        return interval;
    }

    private void updateIntervalEnd(int end) {
        if (intervals.isEmpty()) return;
        Map<String, Object> last = intervals.getLast();
        last.put("endIteration", end);
    }
}