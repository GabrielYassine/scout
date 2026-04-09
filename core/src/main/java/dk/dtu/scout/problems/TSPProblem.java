package dk.dtu.scout.problems;

import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.datatypes.TSPInstance;
import dk.dtu.scout.util.OptimaLookup;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class TSPProblem implements Problem<int[]> {

    private static final String TSP_OPTIMA_RESOURCE = "optima/tsp-optima.properties";
    private static final double EPSILON = 1e-9;
    private static final Map<String, Double> TSP_OPTIMA = OptimaLookup.loadDoubleMap(TSP_OPTIMA_RESOURCE);
    private TSPInstance instance;

    public TSPProblem() {}

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
        if (params == null) {
            return;
        }
        if (params.containsKey("tspInstance")) {
            Object tspInstanceObj = params.get("tspInstance");
            if (!(tspInstanceObj instanceof TSPInstance)) {
                throw new IllegalArgumentException("tspInstance must be a TSPInstance");
            }
            this.instance = (TSPInstance) tspInstanceObj;
        }
    }

    public TSPInstance getInstance() {
        return instance;
    }


    @Override
    public boolean isOptimal(double fitness) {
        Double optimum = OptimaLookup.resolve(TSP_OPTIMA, instance.getName());
        return optimum != null && fitness >= -optimum - EPSILON;
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