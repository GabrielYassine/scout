package dk.dtu.scout.generator;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.datatypes.TSPInstance;
import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.problems.TSP;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
@Scope("prototype")
public class TspAcoGenerator extends AbstractAcoGenerator<int[]> {

    private static final double Q = 1.0;

    private double alpha = 1.0;
    private double beta = 2.0;

    private double minPheromone = 1e-12;
    private double maxPheromone = 1e12;

    private double[][] pheromoneMatrix;
    private State state;
    private TSPInstance tspInstance;

    @Override
    public void init(State state) {
        this.state = state;
        if (state != null) {
            Object problemObj = state.get(StateKeys.PROBLEM);
            if (problemObj instanceof TSP tsp) {
                this.tspInstance = tsp.getInstance();
            }
        }
    }

    @Override
    public String id() {
        return "tsp-aco";
    }

    @Override
    public String displayName() {
        return "TSP ACO Generator";
    }

    @Override
    public String description() {
        return "Ant colony generator for TSP that constructs tours using bounded edge pheromones";
    }

    @Override
    public List<Parameter> params() {
        List<Parameter> params = new java.util.ArrayList<>(evaporationParams());
        params.add(new Parameter("alpha", "Pheromone Influence", "double", alpha, 0.1, 5.0));
        params.add(new Parameter("beta", "Heuristic Influence", "double", beta, 0.1, 10.0));
        params.add(new Parameter("minPheromone", "Minimum Pheromone", "double", minPheromone, 0.0, null));
        params.add(new Parameter("maxPheromone", "Maximum Pheromone", "double", maxPheromone, 0.0, null));
        return params;
    }

    @Override
    public void configure(Map<String, Object> params) {
        super.configure(params);

        if (params == null) {
            return;
        }

        if (params.containsKey("alpha")) {
            double value = ((Number) params.get("alpha")).doubleValue();
            if (value < 0.1 || value > 5.0) {
                throw new IllegalArgumentException("Alpha must be between 0.1 and 5.0");
            }
            this.alpha = value;
        }

        if (params.containsKey("beta")) {
            double value = ((Number) params.get("beta")).doubleValue();
            if (value < 0.1 || value > 10.0) {
                throw new IllegalArgumentException("Beta must be between 0.1 and 10.0");
            }
            this.beta = value;
        }

        if (params.containsKey("minPheromone")) {
            double value = ((Number) params.get("minPheromone")).doubleValue();
            if (value < 0.0) {
                throw new IllegalArgumentException("Minimum pheromone must be non-negative");
            }
            this.minPheromone = value;
        }

        if (params.containsKey("maxPheromone")) {
            double value = ((Number) params.get("maxPheromone")).doubleValue();
            if (value <= 0.0) {
                throw new IllegalArgumentException("Maximum pheromone must be positive");
            }
            this.maxPheromone = value;
        }

        if (minPheromone > maxPheromone) {
            throw new IllegalArgumentException("Minimum pheromone cannot be greater than maximum pheromone");
        }
    }

    @Override
    public int[] generate(Random rng) {
        if (pheromoneMatrix == null) {
            initializePheromoneMatrix();
        }

        int dimension = pheromoneMatrix.length;
        int[] tour = new int[dimension];
        boolean[] visited = new boolean[dimension];

        int current = rng.nextInt(dimension);
        tour[0] = current;
        visited[current] = true;

        for (int step = 1; step < dimension; step++) {
            int next = selectNextCity(current, visited, rng);
            tour[step] = next;
            visited[next] = true;
            current = next;
        }

        return tour;
    }

    private void initializePheromoneMatrix() {
        int dimension = resolveDimension();

        if (dimension <= 0) {
            throw new IllegalStateException("Cannot initialize TspAcoGenerator: dimension must be positive");
        }

        pheromoneMatrix = new double[dimension][dimension];

        double initialPheromone = Math.min(1.0, maxPheromone);
        initialPheromone = Math.max(initialPheromone, minPheromone);

        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                pheromoneMatrix[i][j] = initialPheromone;
            }
        }

        if (state != null) {
            state.update(Map.of(StateKeys.PHEROMONE_MATRIX, pheromoneMatrix));
        }
    }

    private int resolveDimension() {
        if (tspInstance != null) {
            return tspInstance.getDimension();
        }

        if (state != null) {
            Object dimObj = state.get(StateKeys.DIMENSION);
            if (dimObj instanceof Number dimension) {
                return dimension.intValue();
            }
        }

        return 0;
    }

    private int selectNextCity(int current, boolean[] visited, Random rng) {
        int dimension = pheromoneMatrix.length;
        double[] weights = new double[dimension];
        double totalWeight = 0.0;

        for (int city = 0; city < dimension; city++) {
            if (!visited[city]) {
                double pheromone = Math.pow(pheromoneMatrix[current][city], alpha);
                double visibility = Math.pow(1.0 / safeDistance(current, city), beta);
                double weight = pheromone * visibility;

                weights[city] = weight;
                totalWeight += weight;
            }
        }

        if (totalWeight <= 0.0 || Double.isNaN(totalWeight) || Double.isInfinite(totalWeight)) {
            return firstUnvisited(visited);
        }

        double threshold = rng.nextDouble() * totalWeight;
        double cumulative = 0.0;

        for (int city = 0; city < dimension; city++) {
            if (!visited[city]) {
                cumulative += weights[city];
                if (threshold <= cumulative) {
                    return city;
                }
            }
        }

        return firstUnvisited(visited);
    }

    private double safeDistance(int from, int to) {
        if (from == to) {
            return 1e-9;
        }

        if (tspInstance != null) {
            double distance = tspInstance.getDistance(from, to);
            return Math.max(distance, 1e-9);
        }

        return 1.0;
    }

    private int firstUnvisited(boolean[] visited) {
        for (int city = 0; city < visited.length; city++) {
            if (!visited[city]) {
                return city;
            }
        }

        return 0;
    }

    @Override
    public Map<String, Object> getStateVariables(State state) {
        if (pheromoneMatrix == null) {
            initializePheromoneMatrix();
        }

        updatePheromoneMatrix(state);
        return Map.of(StateKeys.PHEROMONE_MATRIX, pheromoneMatrix);
    }

    private void updatePheromoneMatrix(State state) {
        if (state == null || pheromoneMatrix == null || pheromoneMatrix.length == 0) {
            return;
        }

        Object evaluatedObj = state.get(StateKeys.GENERATION_EVALUATED);

        if (!(evaluatedObj instanceof List<?> evaluated) || evaluated.isEmpty()) {
            return;
        }

        evaporate(evaporationRate);

        for (Object entry : evaluated) {
            if (entry instanceof EvaluatedSolution<?>(Object value, double fitness) && value instanceof int[] tour) {
                depositPheromone(tour, fitness);
            }
        }

        clampPheromones();

        if (this.state != null) {
            this.state.update(Map.of(StateKeys.PHEROMONE_MATRIX, pheromoneMatrix));
        }
    }

    private void evaporate(double rate) {
        for (int i = 0; i < pheromoneMatrix.length; i++) {
            for (int j = 0; j < pheromoneMatrix.length; j++) {
                pheromoneMatrix[i][j] *= (1.0 - rate);
            }
        }
    }

    private void depositPheromone(int[] tour, double fitness) {
        double tourLength = -fitness;

        if (tourLength <= 0.0 || Double.isNaN(tourLength) || Double.isInfinite(tourLength)) {
            return;
        }

        double deposit = Q / tourLength;

        for (int i = 0; i < tour.length; i++) {
            int from = tour[i];
            int to = tour[(i + 1) % tour.length];

            pheromoneMatrix[from][to] += deposit;
            pheromoneMatrix[to][from] += deposit;
        }
    }

    private void clampPheromones() {
        for (int i = 0; i < pheromoneMatrix.length; i++) {
            for (int j = 0; j < pheromoneMatrix.length; j++) {
                pheromoneMatrix[i][j] = Math.max(minPheromone, Math.min(maxPheromone, pheromoneMatrix[i][j]));
            }
        }
    }
}