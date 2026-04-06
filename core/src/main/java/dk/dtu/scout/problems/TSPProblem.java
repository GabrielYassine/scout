package dk.dtu.scout.problems;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.datatypes.TSPInstance;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
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
        if (params.containsKey("tspInstance")) {
            Object tspInstanceObj = params.get("tspInstance");
            if (!(tspInstanceObj instanceof TSPInstance)) {
                throw new IllegalArgumentException("tspInstance must be a TSPInstance");
            }
            this.instance = (TSPInstance) tspInstanceObj;
        }
        if (params.containsKey("optimalTourLength")) {
            this.optimalTourLength = ((Number) params.get("optimalTourLength")).doubleValue();
        }
    }
    public TSPInstance getInstance() {
        return instance;
    }

    public double getOptimalTourLength() {
        return optimalTourLength;
    }

    @Override
    public double fitness(int[] tour) {
        if (instance == null) {
            throw new IllegalStateException("TSP instance not configured. Make sure to upload or configure a TSP instance before running.");
        }
        if (tour == null || tour.length != instance.getDimension()) {
            throw new IllegalArgumentException("Tour must have length " + instance.getDimension());
        }

        double tourLength = instance.getTourLength(tour);
        return -tourLength;
    }
}
