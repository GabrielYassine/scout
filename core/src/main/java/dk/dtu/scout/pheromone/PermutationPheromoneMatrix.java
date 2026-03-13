package dk.dtu.scout.pheromone;

import dk.dtu.scout.Parameter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class PermutationPheromoneMatrix implements PheromoneModel<int[]> {

    private double[][] pheromones;
    private int dimension;
    private double initialPheromone = 1.0;

    @Override
    public String id() {
        return "permutation-pheromone";
    }

    @Override
    public String displayName() {
        return "Permutation Pheromone Matrix";
    }

    @Override
    public String description() {
        return "Pheromone matrix for permutation problems, stores pheromone between element pairs";
    }

    @Override
    public List<Parameter> params() {
        return List.of(
            new Parameter("initialPheromone", "Initial pheromone level", "double", initialPheromone, 0.0, null)
        );
    }

    @Override
    public List<String> supportedSearchSpaces() {
        return List.of("permutation");
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) return;
        if (params.containsKey("initialPheromone")) {
            this.initialPheromone = ((Number) params.get("initialPheromone")).doubleValue();
        }
    }

    @Override
    public void initialize(int dimension) {
        this.dimension = dimension;
        this.pheromones = new double[dimension][dimension];

        // Initialize all pheromones to initial value
        for (int i = 0; i < dimension; i++) {
            Arrays.fill(pheromones[i], initialPheromone);
        }
    }

    @Override
    public double getPheromone(int from, int to) {
        if (from < 0 || from >= dimension || to < 0 || to >= dimension) {
            throw new IllegalArgumentException("Invalid indices: from=" + from + ", to=" + to);
        }
        return pheromones[from][to];
    }

    @Override
    public void setPheromone(int from, int to, double value) {
        if (from < 0 || from >= dimension || to < 0 || to >= dimension) {
            throw new IllegalArgumentException("Invalid indices: from=" + from + ", to=" + to);
        }
        pheromones[from][to] = value;
    }

    @Override
    public void evaporate(double evaporationRate) {
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                pheromones[i][j] *= (1.0 - evaporationRate);
            }
        }
    }

    @Override
    public void deposit(int[] solution, double amount) {
        if (solution == null || solution.length != dimension) {
            throw new IllegalArgumentException("Invalid solution length");
        }

        // Deposit pheromone on edges in the tour
        for (int i = 0; i < solution.length; i++) {
            int from = solution[i];
            int to = solution[(i + 1) % solution.length]; // Wrap around to create cycle
            pheromones[from][to] += amount;
            pheromones[to][from] += amount; // Symmetric for TSP
        }
    }

    @Override
    public int getDimension() {
        return dimension;
    }
}