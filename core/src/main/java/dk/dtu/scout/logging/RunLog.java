package dk.dtu.scout.logging;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregated time-series output for a run, populated by observers.
 * @author s235257 & s230632
 */
public class RunLog {
    private final List<Integer> evaluations = new ArrayList<>();
    private final Map<String, LoggedSeries<?>> series = new LinkedHashMap<>();

    public void tick(int evaluation) {
        evaluations.add(evaluation);
    }

    public <T> void putSeries(String key, T value, SeriesMode mode) {
        @SuppressWarnings("unchecked")
        LoggedSeries<T> loggedSeries = (LoggedSeries<T>) series.computeIfAbsent(key, k -> new LoggedSeries<>(mode));
        loggedSeries.add(value);
    }

    public List<Integer> getEvaluations() { return evaluations; }
    public Map<String, LoggedSeries<?>> getSeries() { return series; }
}
