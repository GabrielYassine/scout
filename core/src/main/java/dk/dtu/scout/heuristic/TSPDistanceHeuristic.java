package dk.dtu.scout.heuristic;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.problems.TSPInstance;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TSPDistanceHeuristic implements HeuristicFunction<int[]> {

    private double[][] heuristics;
    private int dimension;

    @Override
    public String id() {
        return "tsp-distance";
    }

    @Override
    public String displayName() {
        return "TSP Distance Heuristic";
    }

    @Override
    public String description() {
        return "Heuristic based on inverse distance between cities (closer = better)";
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
    public void initialize(Object problemData) {
        TSPInstance instance = null;

        // Handle both direct TSPInstance and TSPProblem
        if (problemData instanceof TSPInstance) {
            instance = (TSPInstance) problemData;
        } else if (problemData instanceof dk.dtu.scout.problems.TSPProblem) {
            dk.dtu.scout.problems.TSPProblem tspProblem = (dk.dtu.scout.problems.TSPProblem) problemData;
            instance = tspProblem.getInstance();
        }

        if (instance == null) {
            throw new IllegalArgumentException("TSPDistanceHeuristic requires TSPInstance or TSPProblem with configured instance");
        }

        this.dimension = instance.getDimension();
        this.heuristics = new double[dimension][dimension];

        // Compute heuristic as inverse of distance
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                if (i == j) {
                    heuristics[i][j] = 0.0;
                } else {
                    double distance = instance.getDistance(i, j);
                    heuristics[i][j] = 1.0 / (distance + 1e-10); // Add small value to avoid division by zero
                }
            }
        }
    }

    @Override
    public double getHeuristic(int from, int to) {
        if (from < 0 || from >= dimension || to < 0 || to >= dimension) {
            throw new IllegalArgumentException("Invalid indices: from=" + from + ", to=" + to);
        }
        return heuristics[from][to];
    }
}
