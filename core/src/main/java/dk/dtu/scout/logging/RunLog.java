package dk.dtu.scout.logging;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RunLog {
    private final List<Integer> iterations = new ArrayList<>();
    private final List<Integer> evaluations = new ArrayList<>();
    private final Map<String, List<Double>> series = new LinkedHashMap<>();

    public void tick(int iteration, int evaluation) {
        iterations.add(iteration);
        evaluations.add(evaluation);
    }

    public void put(String key, double value) {
        series.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    public List<Integer> getIterations() { return iterations; }
    public List<Integer> getEvaluations() { return evaluations; }
    public Map<String, List<Double>> getSeries() { return series; }
}

