package dk.dtu.scout.problems;

import dk.dtu.scout.Parameter;
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
            if (tspInstanceObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> tspInstanceMap = (Map<String, Object>) tspInstanceObj;
                this.instance = convertMapToInstance(tspInstanceMap);
            }
        }
        else if (params.containsKey("instance")) {
            this.instance = (TSPInstance) params.get("instance");
        }
        if (params.containsKey("optimalTourLength")) {
            this.optimalTourLength = ((Number) params.get("optimalTourLength")).doubleValue();
        }
    }

    private TSPInstance convertMapToInstance(Map<String, Object> tspInstanceMap) {
        String name = (String) tspInstanceMap.getOrDefault("name", "Custom Instance");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> citiesList = (List<Map<String, Object>>) tspInstanceMap.get("cities");

        if (citiesList == null || citiesList.isEmpty()) {
            throw new IllegalArgumentException("TSP instance must have cities");
        }

        int dimension = citiesList.size();
        double[][] coordinates = new double[dimension][2];

        for (int i = 0; i < dimension; i++) {
            Map<String, Object> city = citiesList.get(i);
            Object xObj = city.get("x");
            Object yObj = city.get("y");

            double x = (xObj instanceof Number) ? ((Number) xObj).doubleValue() : 0.0;
            double y = (yObj instanceof Number) ? ((Number) yObj).doubleValue() : 0.0;

            coordinates[i][0] = x;
            coordinates[i][1] = y;
        }

        return new TSPInstance(name, dimension, coordinates);
    }

    public void setInstance(TSPInstance instance) {
        this.instance = instance;
    }

    public TSPInstance getInstance() {
        return instance;
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

    @Override
    public boolean isOptimal(double fitness) {
        if (optimalTourLength == Double.NEGATIVE_INFINITY) {
            return false;
        }
        return Math.abs(fitness - (-optimalTourLength)) < 0.001;
    }
}
