package dk.dtu.scout.logging;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RunLog {
    private final List<Integer> iterations = new ArrayList<>();
    private final List<Integer> evaluations = new ArrayList<>();
    private final Map<String, LoggedSeries<?>> series = new LinkedHashMap<>();

    public void tick(int iteration, int evaluation) {
        iterations.add(iteration);
        evaluations.add(evaluation);
    }

    public <T> void putSeries(String key, T value) {
        putSeries(key, value, SeriesMode.ALL);
    }

    public <T> void putSeries(String key, T value, SeriesMode mode) {
        @SuppressWarnings("unchecked")
        LoggedSeries<T> loggedSeries =
                (LoggedSeries<T>) series.computeIfAbsent(key, k -> new LoggedSeries<>(mode));
        loggedSeries.add(value);
    }

    public List<Integer> getIterations() { return iterations; }
    public List<Integer> getEvaluations() { return evaluations; }
    public Map<String, LoggedSeries<?>> getSeries() { return series; }
}

