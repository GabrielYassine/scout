package dk.dtu.scout.logging;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RunLog {
    private final List<Integer> iterations = new ArrayList<>();
    private final List<Integer> evaluations = new ArrayList<>();
    private final Map<String, List<?>> series = new LinkedHashMap<>();

    public void tick(int iteration, int evaluation) {
        iterations.add(iteration);
        evaluations.add(evaluation);
    }

    public void put(String key, double value) {
        @SuppressWarnings("unchecked")
        List<Double> list = (List<Double>) series.computeIfAbsent(key, k -> new ArrayList<Double>());
        list.add(value);
    }

    public <T> void putSeries(String key, T value) {
        @SuppressWarnings("unchecked")
        List<T> list = (List<T>) series.computeIfAbsent(key, k -> new ArrayList<T>());
        list.add(value);
    }

    public List<Integer> getIterations() { return iterations; }
    public List<Integer> getEvaluations() { return evaluations; }
    public Map<String, List<?>> getSeries() { return series; }
}

