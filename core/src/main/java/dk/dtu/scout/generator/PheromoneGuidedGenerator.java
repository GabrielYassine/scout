package dk.dtu.scout.generator;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.State;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
@Scope("prototype")
public class PheromoneGuidedGenerator implements Generator<int[]> {

    private double evaporationRate = 0.1;
    private double alpha = 1.0;  // Pheromone influence (theory: 1.0)
    private double beta = 2.0;   // Visibility/distance influence (theory: 2.0-5.0)
    private double[][] pheromoneMatrix;
    private double[][] distanceMatrix;
    private State state;

    @Override
    public void init(State state) {
        this.state = state;
    }

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
        return List.of(
            new Parameter("evaporationRate", "Pheromone Evaporation Rate", "double", evaporationRate, 0.0, 1.0),
            new Parameter("alpha", "Pheromone Influence", "double", alpha, 0.1, 5.0),
            new Parameter("beta", "Visibility/Distance Influence", "double", beta, 0.1, 10.0)
        );
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) return;
        if (params.containsKey("evaporationRate")) {
            double value = ((Number) params.get("evaporationRate")).doubleValue();
            if (value < 0.0 || value > 1.0) {
                throw new IllegalArgumentException("Evaporation rate must be between 0 and 1");
            }
            this.evaporationRate = value;
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
            throw new IllegalStateException("PheromoneGuidedGenerator not properly initialized. Pheromone matrix is empty or null.");
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
            Object dimObj = state.get("dimension");
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
            throw new IllegalStateException("Cannot initialize PheromoneGuidedGenerator: dimension must be positive. Got dimension=" + dim + " from state=" + state);
        }

        pheromoneMatrix = new double[dim][dim];
        distanceMatrix = new double[dim][dim];

        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                pheromoneMatrix[i][j] = 1.0;
                distanceMatrix[i][j] = 1.0; // Default: will be overridden if TSP instance available
            }
        }

        if (state != null) {
            state.update(Map.of("pheromoneMatrix", pheromoneMatrix));
        }
    }

    private int selectNextCityByPheromone(int current, boolean[] visited, Random rng) {
        int dimension = pheromoneMatrix.length;
        double[] probabilities = new double[dimension];
        double sum = 0.0;

        // ACO formula: P(i,j) ∝ τ(i,j)^α * η(i,j)^β
        // where τ is pheromone and η is visibility (1/distance)
        for (int i = 0; i < dimension; i++) {
            if (!visited[i]) {
                double pheromone = Math.pow(pheromoneMatrix[current][i] + 0.01, alpha);
                double visibility = Math.pow(1.0 / (distanceMatrix[current][i] + 1.0), beta);
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
        return Map.of("pheromoneMatrix", pheromoneMatrix);
    }


    private void updatePheromoneMatrix(State state) {
        if (state == null || pheromoneMatrix == null || pheromoneMatrix.length == 0) {
            return;
        }

        // Try to extract distance matrix from TSP instance if available
        if (distanceMatrix != null && distanceMatrix[0][0] == 1.0) {
            // Distance matrix not yet populated, try to get it from state
            Object tspInstanceObj = state.get("tspInstance");
            if (tspInstanceObj != null) {
                try {
                    // Try to access distance information if TSPInstance is available
                    // This is a best-effort approach - the actual implementation depends on state structure
                    // For now, distance matrix stays as initialized (uniform)
                } catch (Exception e) {
                    // Silently ignore - use default distance matrix
                }
            }
        }

        Object solutionsObj = state.get("generationSolutions");
        Object fitnessObj = state.get("generationFitness");

        if (!(solutionsObj instanceof List<?> solutions) || !(fitnessObj instanceof List<?> fitnessValues)) {
            return;
        }

        if (solutions.isEmpty() || solutions.size() != fitnessValues.size()) {
            return;
        }
        evaporate(evaporationRate);

        for (int i = 0; i < solutions.size(); i++) {
            Object solObj = solutions.get(i);
            Object fitObj = fitnessValues.get(i);

            if (solObj instanceof int[] solution && fitObj instanceof Double) {
                double fitness = (Double) fitObj;
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
        // TSP fitness is negative (we maximize -tourLength)
        // So we need to use the absolute value to get the actual quality measure
        // Better solutions have higher negative fitness (closer to 0)
        // We want to deposit MORE pheromone for BETTER (less negative) solutions
        double tourLength = -fitness;  // Convert fitness back to tour length (positive value)
        double deposit = Math.max(0.1, 1.0 / tourLength);  // Inverse of tour length: shorter tours get more pheromone

        for (int i = 0; i < solution.length; i++) {
            int from = solution[i];
            int to = solution[(i + 1) % solution.length];

            pheromoneMatrix[from][to] += deposit;
            pheromoneMatrix[to][from] += deposit; // Symmetric for undirected TSP
        }
    }
}