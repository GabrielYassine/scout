package dk.dtu.scout.mutation;

import dk.dtu.scout.Parameter;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class PheromoneGuidedGenerator implements Generator<int[]> {

    private double[][] pheromoneMatrix = null;

    @Override
    public String id() {
        return "pheromone-guided";
    }

    @Override
    public String displayName() {
        return "Pheromone-Guided Mutation";
    }

    @Override
    public String description() {
        return "Generate a new solution guided by pheromone information";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public int[] generate(int[] solution, Random rng) {
        int dimension = pheromoneMatrix.length;
        int[] result = new int[dimension];
        boolean[] visited = new boolean[dimension];

        // Start from a random city
        int current = rng.nextInt(dimension);
        result[0] = current;
        visited[current] = true;

        // Build the permutation based on pheromone values
        for (int step = 1; step < dimension; step++) {
            int next = selectNextCityByPheromone(current, visited, rng);
            result[step] = next;
            visited[next] = true;
            current = next;
        }

        return result;
    }

    private int selectNextCityByPheromone(int current, boolean[] visited, Random rng) {
        int dimension = pheromoneMatrix.length;
        double[] probabilities = new double[dimension];
        double sum = 0.0;

        // Calculate probabilities based on pheromone levels
        for (int i = 0; i < dimension; i++) {
            if (!visited[i]) {
                probabilities[i] = pheromoneMatrix[current][i];
                sum += probabilities[i];
            }
        }

        // Normalize probabilities
        if (sum > 0) {
            for (int i = 0; i < dimension; i++) {
                probabilities[i] /= sum;
            }
        }

        // Select next city using roulette wheel selection
        double rand = rng.nextDouble();
        double cumulativeProbability = 0.0;
        for (int i = 0; i < dimension; i++) {
            if (!visited[i]) {
                cumulativeProbability += probabilities[i];
                if (rand <= cumulativeProbability) {
                    return i;
                }
            }
        }

        // Fallback: return first unvisited city (should not happen)
        for (int i = 0; i < dimension; i++) {
            if (!visited[i]) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public Map<String, Object> getStateVariables() {
        return Map.of(
            "pheromoneMatrix", pheromoneMatrix
        );
    }
}