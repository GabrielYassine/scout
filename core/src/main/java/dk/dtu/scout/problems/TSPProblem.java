package dk.dtu.scout.problems;

import dk.dtu.scout.Parameter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TSPProblem implements Problem<int[]> {

    private TSPInstance instance;
    private double optimalTourLength = Double.NEGATIVE_INFINITY;

    public TSPProblem() {
    }

    @Override
    public String id() {
        return "tsp";
    }

    @Override
    public String displayName() {
        return "Traveling Salesman Problem";
    }

    @Override
    public String description() {
        return "Find the shortest tour visiting all cities exactly once and returning to start";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public List<String> supportedSearchSpaces() {
        return List.of("permutation");
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) return;
        if (params.containsKey("instance")) {
            this.instance = (TSPInstance) params.get("instance");
        }
        if (params.containsKey("optimalTourLength")) {
            this.optimalTourLength = ((Number) params.get("optimalTourLength")).doubleValue();
        }
    }

    public void setInstance(TSPInstance instance) {
        this.instance = instance;
    }

    public TSPInstance getInstance() {
        return instance;
    }

    @Override
    public double fitness(int[] tour) {
        if (tour == null || tour.length != instance.getDimension()) {
            throw new IllegalArgumentException("Tour must have length " + instance.getDimension());
        }

        double tourLength = instance.getTourLength(tour);
        return -tourLength;
    }

    @Override
    public boolean isOptimal(double fitness) {
        if (optimalTourLength == Double.NEGATIVE_INFINITY) {
            return false;
        }
        return Math.abs(fitness - (-optimalTourLength)) < 0.001;
    }
}
