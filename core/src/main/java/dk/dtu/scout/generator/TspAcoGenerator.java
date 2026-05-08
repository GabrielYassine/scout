package dk.dtu.scout.generator;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.datatypes.TSPInstance;
import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.problems.TSP;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Ant colony optimization generator for TSP that constructs tours using bounded edge pheromones.
 * Provides parameters for evaporation rate, reinforcement rate, pheromone influence (alpha), heuristic influence (beta),
 * minimum and maximum pheromone levels, reinforcement mode (best-so-far, iteration-best, or all),
 * and whether to accept equal fitness as best-so-far.
 * @author s235257
 */
@Component
@Scope("prototype")
public class TspAcoGenerator implements Generator<int[]> {

    private static final double Q = 1.0;

    private double evaporationRate = 0.1;
    private double reinforcementRate = 1.0;

    private double alpha = 1.0;
    private double beta = 2.0;

    private double minPheromone = 1e-12;
    private double maxPheromone = 1e12;

    private ReinforcementMode reinforcementMode = ReinforcementMode.BEST_SO_FAR;
    private boolean acceptEqualFitness = true;

    private double[][] pheromoneMatrix;
    private State state;
    private TSPInstance tspInstance;
    private EvaluatedSolution<int[]> bestSoFar;

    @Override
    public void init(State state) {
        this.state = state;
        this.tspInstance = ((TSP) state.get(StateKeys.PROBLEM)).getInstance();
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
        return List.of(
            new Parameter("evaporationRate", "Pheromone Evaporation Rate", "double", evaporationRate, 0.0, 1.0, null),
            new Parameter("reinforcementRate", "Pheromone Reinforcement Rate", "double", reinforcementRate, 0.0, null, null),
            new Parameter("alpha", "Pheromone Influence", "double", alpha, 0.1, 5.0, null),
            new Parameter("beta", "Heuristic Influence", "double", beta, 0.1, 10.0, null),
            new Parameter("minPheromone", "Minimum Pheromone", "double", minPheromone, 0.0, null, null),
            new Parameter("maxPheromone", "Maximum Pheromone", "double", maxPheromone, 0.0, null, null),
            new Parameter("reinforcementMode", "Reinforcement Mode", "enum", reinforcementMode.name(), null, null, List.of("BEST_SO_FAR", "ITERATION_BEST", "ALL")),
            new Parameter("acceptEqualFitness", "Accept Equal Fitness", "boolean", acceptEqualFitness, null, null, null)
        );
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params.containsKey("evaporationRate")) {
            evaporationRate = resolveRate(params.get("evaporationRate"), "Evaporation rate");
        }

        if (params.containsKey("reinforcementRate")) {
            reinforcementRate = resolveNonNegative(params.get("reinforcementRate"), "Reinforcement rate");
        }

        if (params.containsKey("alpha")) {
            alpha = resolveRange(params.get("alpha"), 0.1, 5.0, "Alpha");
        }

        if (params.containsKey("beta")) {
            beta = resolveRange(params.get("beta"), 0.1, 10.0, "Beta");
        }

        if (params.containsKey("minPheromone")) {
            minPheromone = resolveNonNegative(params.get("minPheromone"), "Minimum pheromone");
        }

        if (params.containsKey("maxPheromone")) {
            maxPheromone = resolvePositive(params.get("maxPheromone"));
        }

        boolean hasReinforcementMode = params.containsKey("reinforcementMode");

        if (hasReinforcementMode) {
            reinforcementMode = parseReinforcementMode(params.get("reinforcementMode"));
        }

        if (params.containsKey("acceptEqualFitness")) {
            acceptEqualFitness = (Boolean) params.get("acceptEqualFitness");
        }

        validatePheromoneBounds();
    }

    @Override
    public List<String> supportedSearchSpaces() {
        return List.of("permutation");
    }
    /**
     * Generates a TSP tour by constructing a path through the cities based on pheromone levels and heuristic visibility.
     * @param rng random number generator for stochastic choices
     * @return generated TSP tour as an array of city indices
     */
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
    /**
     * Updates the pheromone matrix based on the evaluated solutions in the current generation and returns it as a state variable.
     * The update is performed according to the selected reinforcement mode (best-so-far, iteration-best, or all).
     * @param state current state containing evaluated solutions and other relevant information
     * @return map of state variables including the updated pheromone matrix
     */
    @Override
    public Map<String, Object> getStateVariables(State state) {
        if (pheromoneMatrix == null) {
            initializePheromoneMatrix();
        }

        updatePheromoneMatrix(state);
        this.state.update(Map.of(StateKeys.PHEROMONE_MATRIX, pheromoneMatrix));

        return Map.of(StateKeys.PHEROMONE_MATRIX, pheromoneMatrix);
    }

    private void initializePheromoneMatrix() {
        int dimension = tspInstance.getDimension();
        double initialPheromone = Math.max(minPheromone, Math.min(maxPheromone, 1.0));

        pheromoneMatrix = new double[dimension][dimension];

        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                pheromoneMatrix[i][j] = initialPheromone;
            }
        }

        state.update(Map.of(StateKeys.PHEROMONE_MATRIX, pheromoneMatrix));
    }

    /**
     * Selects the next city to visit based on the pheromone levels and heuristic visibility of the candidate cities.
     * @param current current city index
     * @param visited boolean array indicating which cities have been visited
     * @param rng random number generator for stochastic selection
     * @return index of the selected next city
     */
    private int selectNextCity(int current, boolean[] visited, Random rng) {
        List<Integer> candidates = new ArrayList<>();

        for (int city = 0; city < visited.length; city++) {
            if (!visited[city]) {
                candidates.add(city);
            }
        }

        double[] weights = new double[candidates.size()];
        double totalWeight = 0.0;

        for (int i = 0; i < candidates.size(); i++) {
            int city = candidates.get(i);
            double pheromone = Math.pow(pheromoneMatrix[current][city], alpha);
            double visibility = Math.pow(1.0 / safeDistance(current, city), beta);
            double weight = pheromone * visibility;

            weights[i] = weight;
            totalWeight += weight;
        }

        if (totalWeight <= 0.0) {
            return candidates.getFirst();
        }

        double threshold = rng.nextDouble() * totalWeight;
        double cumulative = 0.0;

        for (int i = 0; i < weights.length - 1; i++) {
            cumulative += weights[i];

            if (threshold <= cumulative) {
                return candidates.get(i);
            }
        }

        return candidates.getLast();
    }

    private double safeDistance(int from, int to) {
        return Math.max(tspInstance.getDistance(from, to), 1e-9);
    }
    /**
     * Updates the pheromone matrix based on the evaluated solutions in the current generation according to the selected reinforcement mode.
     * @param state current state containing evaluated solutions and other relevant information
     */
    private void updatePheromoneMatrix(State state) {
        Object evaluatedObj = state.get(StateKeys.GENERATION_EVALUATED);

        if (!(evaluatedObj instanceof List<?> evaluated) || evaluated.isEmpty()) {
            return;
        }

        switch (reinforcementMode) {
            case BEST_SO_FAR -> reinforceBestSoFar(evaluated);
            case ITERATION_BEST -> reinforceIterationBest(evaluated);
            case ALL -> reinforceAll(evaluated);
        }

        clampPheromones();
    }

    /**
     * Reinforces the pheromone matrix using the best solution found so far across all generations.
     * Updates the best-so-far solution if a better one is found in the current generation.
     * @param evaluated list of evaluated solutions from the current generation
     */

    private void reinforceBestSoFar(List<?> evaluated) {
        updateBestSoFar(evaluated);
        evaporate();
        depositPheromone(bestSoFar.value(), bestSoFar.fitness(), reinforcementRate);
    }
    /**
     * Reinforces the pheromone matrix using the best solution found in the current generation.
     * @param evaluated list of evaluated solutions from the current generation
     */

    private void reinforceIterationBest(List<?> evaluated) {
        EvaluatedSolution<int[]> generationBest = bestOf(evaluated);
        evaporate();
        depositPheromone(generationBest.value(), generationBest.fitness(), reinforcementRate);
    }
    /**
     * Reinforces the pheromone matrix using all evaluated solutions from the current generation, scaled by the number of solutions.
     * @param evaluated list of evaluated solutions from the current generation
     */

    @SuppressWarnings("unchecked")
    private void reinforceAll(List<?> evaluated) {
        evaporate();

        double scaledRate = reinforcementRate / evaluated.size();

        for (Object entry : evaluated) {
            EvaluatedSolution<int[]> solution = (EvaluatedSolution<int[]>) entry;
            depositPheromone(solution.value(), solution.fitness(), scaledRate);
        }
    }

    /**
     * Updates the best-so-far solution if a better solution is found in the current generation.
     * @param evaluated list of evaluated solutions from the current generation
     */
    private void updateBestSoFar(List<?> evaluated) {
        EvaluatedSolution<int[]> generationBest = bestOf(evaluated);
        if (bestSoFar == null || isAcceptedAsBestSoFar(generationBest)) {
            bestSoFar = new EvaluatedSolution<>(generationBest.value().clone(), generationBest.fitness());
        }
    }

    private boolean isAcceptedAsBestSoFar(EvaluatedSolution<int[]> candidate) {
        return acceptEqualFitness ? candidate.fitness() >= bestSoFar.fitness() : candidate.fitness() > bestSoFar.fitness();
    }

    /**
     * Finds the best solution from a list of evaluated solutions based on fitness.
     * @param evaluated list of evaluated solutions
     * @return the solution with the highest fitness
     */
    @SuppressWarnings("unchecked")
    private EvaluatedSolution<int[]> bestOf(List<?> evaluated) {
        EvaluatedSolution<int[]> best = (EvaluatedSolution<int[]>) evaluated.getFirst();

        for (int i = 1; i < evaluated.size(); i++) {
            EvaluatedSolution<int[]> candidate = (EvaluatedSolution<int[]>) evaluated.get(i);

            if (candidate.fitness() > best.fitness()) {
                best = candidate;
            }
        }

        return best;
    }
    /**
     * Deposits pheromone on the edges of the given tour based on its fitness and the specified reinforcement rate.
     * @param fitness fitness value of the tour (negative tour length)
     * @param rate reinforcement rate to scale the pheromone deposit
     */
    private void depositPheromone(int[] tour, double fitness, double rate) {
        double tourLength = -fitness;
        double deposit = (rate * Q) / tourLength;

        for (int i = 0; i < tour.length; i++) {
            int from = tour[i];
            int to = tour[(i + 1) % tour.length];

            pheromoneMatrix[from][to] += deposit;
            pheromoneMatrix[to][from] += deposit;
        }
    }
    /**
     * Applies evaporation to the pheromone matrix by reducing all pheromone levels according to the evaporation rate.
     */
    private void evaporate() {
        for (int i = 0; i < pheromoneMatrix.length; i++) {
            for (int j = 0; j < pheromoneMatrix.length; j++) {
                pheromoneMatrix[i][j] *= (1.0 - evaporationRate);
            }
        }
    }
    /**
     * Clamps the pheromone levels in the matrix to be within the specified minimum and maximum.
     */
    private void clampPheromones() {
        for (int i = 0; i < pheromoneMatrix.length; i++) {
            for (int j = 0; j < pheromoneMatrix.length; j++) {
                pheromoneMatrix[i][j] = Math.max(minPheromone, Math.min(maxPheromone, pheromoneMatrix[i][j]));
            }
        }
    }

    /**
     * Resolves a parameter value as a rate between 0 and 1, accepting either a numeric value or a formula string.
     * @param value parameter value to resolve, expected to be either a Number or a String formula
     * @param label human-readable label for error messages
     * @return resolved rate as a double between 0.0 and 1.0
     */
    private double resolveRate(Object value, String label) {
        return resolveRange(value, 0.0, 1.0, label);
    }

    /**
     * Resolves a parameter value as a double within a specified range, accepting either a numeric value or a formula string.
     * @param value parameter value to resolve, expected to be either a Number or a String formula
     * @param min minimum allowed value for the parameter
     * @param max maximum allowed value for the parameter
     * @param label human-readable label for error messages
     * @return resolved value as a double within the specified range
     */
    private double resolveRange(Object value, double min, double max, String label) {
        double resolved = ((Number) value).doubleValue();

        if (resolved < min || resolved > max) {
            throw new IllegalArgumentException(label + " must be between " + min + " and " + max);
        }

        return resolved;
    }
    /**
     * Resolves a parameter value as a non-negative double, accepting either a numeric value or a formula string.
     * @param value parameter value to resolve, expected to be either a Number or a String formula
     * @param label human-readable label for error messages
     * @return resolved value as a non-negative double
     */
    private double resolveNonNegative(Object value, String label) {
        double resolved = ((Number) value).doubleValue();

        if (resolved < 0.0) {
            throw new IllegalArgumentException(label + " must be non-negative");
        }

        return resolved;
    }
    /**
     * Resolves a parameter value as a positive double, accepting either a numeric value or a formula string.
     * @param value parameter value to resolve, expected to be either a Number or a String formula
     * @return resolved value as a positive double
     */

    private double resolvePositive(Object value) {
        double resolved = ((Number) value).doubleValue();

        if (resolved <= 0.0) {
            throw new IllegalArgumentException("Maximum pheromone" + " must be positive");
        }

        return resolved;
    }
    /**
     * Validates that the minimum pheromone level is not greater than the maximum pheromone level.
     * Throws an IllegalArgumentException if the bounds are invalid.
     */
    private void validatePheromoneBounds() {
        if (minPheromone > maxPheromone) {
            throw new IllegalArgumentException("Minimum pheromone cannot be greater than maximum pheromone");
        }
    }
    /**
     * Parses the reinforcement mode from a parameter value, accepting either a string representation of the enum or an actual enum value.
     * @param value parameter value to parse, expected to be either a String or a ReinforcementMode
     * @return parsed ReinforcementMode enum value
     */

    private ReinforcementMode parseReinforcementMode(Object value) {
        String normalized = value.toString().trim().toUpperCase();

        try {
            return ReinforcementMode.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid reinforcement mode: " + value, ex);
        }
    }
}