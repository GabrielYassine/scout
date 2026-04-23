package dk.dtu.scout.generator;

import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.datatypes.TSPInstance;
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

    private double[][] pheromoneMatrix;
    private double[][] distanceMatrix;
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
        return "Ant colony generator for TSP that constructs tours using edge pheromones";
    }

    @Override
    public List<Parameter> params() {
        List<Parameter> params = new java.util.ArrayList<>(evaporationParams());
        params.add(new Parameter("alpha", "Pheromone Influence", "double", alpha, 0.1, 5.0));
        params.add(new Parameter("beta", "Heuristic Influence", "double", beta, 0.1, 10.0));
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
    }

    @Override
    public int[] generate(Random rng) {
        if (pheromoneMatrix == null) {
            initializePheromoneMatrix();
        }

        if (pheromoneMatrix == null || pheromoneMatrix.length == 0) {
            throw new IllegalStateException("TspAcoGenerator not properly initialized. Pheromone matrix is empty or null.");
        }

        int dim = pheromoneMatrix.length;

        int[] result = new int[dim];
        boolean[] visited = new boolean[dim];

        // Start from a random city
        int current = rng.nextInt(dim);
        result[0] = current;
        visited[current] = true;

        // Build the tour guided by pheromone levels
        for (int step = 1; step < dim; step++) {
            int next = selectNextCityByPheromone(current, visited, rng);
            result[step] = next;
            visited[next] = true;
            current = next;
        }

        return result;
    }

    private void initializePheromoneMatrix() {
        int dim = 0;

        if (state != null) {
            Object dimObj = state.get(StateKeys.DIMENSION);
            if (dimObj instanceof Integer) {
                dim = (Integer) dimObj;
            }
            if (dim <= 0) {
                Object nObj = state.get("n");
                if (nObj instanceof Integer) {
                    dim = (Integer) nObj;
                }
            }
        }

        if (dim <= 0) {
            throw new IllegalStateException("Cannot initialize TspAcoGenerator: dimension must be positive. Got dimension=" + dim + " from state=" + state);
        }

        pheromoneMatrix = new double[dim][dim];
        distanceMatrix = new double[dim][dim];

        // Initialize pheromone matrix with uniform values
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                pheromoneMatrix[i][j] = 1.0;
            }
        }

        // load distance matrix if available
        if (tspInstance != null) {
            double[][] realDistances = tspInstance.getCoordinates();
            if (realDistances != null && realDistances.length == dim) {
                // Compute Euclidean distances
                for (int i = 0; i < dim; i++) {
                    for (int j = 0; j < dim; j++) {
                        if (i == j) {
                            distanceMatrix[i][j] = 0.0;
                        } else {
                            double dx = realDistances[i][0] - realDistances[j][0];
                            double dy = realDistances[i][1] - realDistances[j][1];
                            distanceMatrix[i][j] = Math.sqrt(dx * dx + dy * dy);
                        }
                    }
                }
            }
        }

        if (state != null) {
            state.update(Map.of(StateKeys.PHEROMONE_MATRIX, pheromoneMatrix));
        }
    }

    private int selectNextCityByPheromone(int current, boolean[] visited, Random rng) {
        int dimension = pheromoneMatrix.length;
        double[] probabilities = new double[dimension];
        double sum = 0.0;

        // Calculate probabilities based on pheromone and visibility
        for (int i = 0; i < dimension; i++) {
            if (!visited[i]) {
                double pheromone = Math.pow(pheromoneMatrix[current][i], alpha);
                double distance = distanceMatrix[current][i];
                double visibility = Math.pow(1.0 / (distance + 0.001), beta);
                probabilities[i] = pheromone * visibility;
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

        // Fallback: return first unvisited city
        for (int i = 0; i < dimension; i++) {
            if (!visited[i]) {
                return i;
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

        if (!(evaluatedObj instanceof List<?> evaluated)) {
            return;
        }

        if (evaluated.isEmpty()) {
            return;
        }

        // Evaporate pheromone
        evaporate(evaporationRate);

        // deposit pheromone from all generation solutions
        for (Object entry : evaluated) {
            if (entry instanceof EvaluatedSolution<?>(Object value, double fitness) && value instanceof int[] solution) {
                depositPheromone(solution, fitness);
            }
        }
    }

    private void evaporate(double rate) {
        for (int i = 0; i < pheromoneMatrix.length; i++) {
            for (int j = 0; j < pheromoneMatrix.length; j++) {
                pheromoneMatrix[i][j] *= (1.0 - rate);
            }
        }
    }

    private void depositPheromone(int[] solution, double fitness) {
        double tourLength = -fitness;  // Convert fitness back to tour length (positive value)

        double deposit = Q / tourLength; // from ACO theory: deltaT = Q / L, where L is the tour length

        for (int i = 0; i < solution.length; i++) {
            int from = solution[i];
            int to = solution[(i + 1) % solution.length];

            pheromoneMatrix[from][to] += deposit;
            pheromoneMatrix[to][from] += deposit; // Symmetric for undirected TSP
        }
    }
}
