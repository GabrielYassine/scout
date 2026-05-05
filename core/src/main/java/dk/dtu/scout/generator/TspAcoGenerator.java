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

        Object problemObj = state.get(StateKeys.PROBLEM);
        if (problemObj instanceof TSP tsp) {
            this.tspInstance = tsp.getInstance();
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
        List<Parameter> params = new ArrayList<>();
        params.add(new Parameter("evaporationRate", "Pheromone Evaporation Rate", "double", evaporationRate, 0.0, 1.0, null));
        params.add(new Parameter("reinforcementRate", "Pheromone Reinforcement Rate", "double", reinforcementRate, 0.0, null, null));
        params.add(new Parameter("alpha", "Pheromone Influence", "double", alpha, 0.1, 5.0, null));
        params.add(new Parameter("beta", "Heuristic Influence", "double", beta, 0.1, 10.0, null));
        params.add(new Parameter("minPheromone", "Minimum Pheromone", "double", minPheromone, 0.0, null, null));
        params.add(new Parameter("maxPheromone", "Maximum Pheromone", "double", maxPheromone, 0.0, null, null));
        params.add(new Parameter("reinforcementMode", "Reinforcement Mode", "enum", reinforcementMode.name(), null, null, List.of("BEST_SO_FAR", "ITERATION_BEST", "ALL")));
        params.add(new Parameter("acceptEqualFitness", "Accept Equal Fitness", "boolean", acceptEqualFitness, null, null, null));
        return params;
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params.containsKey("evaporationRate")) {
            double value = ((Number) params.get("evaporationRate")).doubleValue();
            if (value < 0.0 || value > 1.0) {
                throw new IllegalArgumentException("Evaporation rate must be between 0 and 1");
            }
            this.evaporationRate = value;
        }

        if (params.containsKey("reinforcementRate")) {
            double value = ((Number) params.get("reinforcementRate")).doubleValue();
            if (value < 0.0) {
                throw new IllegalArgumentException("Reinforcement rate must be non-negative");
            }
            this.reinforcementRate = value;
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

        boolean hasReinforcementMode = params.containsKey("reinforcementMode");
        if (hasReinforcementMode) {
            this.reinforcementMode = parseReinforcementMode(params.get("reinforcementMode"));
        }

        if (params.containsKey("reinforceBestOnly") && !hasReinforcementMode) {
            boolean reinforceBestOnly = (Boolean) params.get("reinforceBestOnly");
            this.reinforcementMode = reinforceBestOnly ? ReinforcementMode.ITERATION_BEST : ReinforcementMode.ALL;
        }

        if (params.containsKey("acceptEqualFitness")) {
            this.acceptEqualFitness = (Boolean) params.get("acceptEqualFitness");
        }

        if (minPheromone > maxPheromone) {
            throw new IllegalArgumentException("Minimum pheromone cannot be greater than maximum pheromone");
        }
    }

    @Override
    public List<String> supportedSearchSpaces() {
        return List.of("permutation");
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

        state.update(Map.of(StateKeys.PHEROMONE_MATRIX, pheromoneMatrix));
    }

    private int resolveDimension() {
        if (tspInstance != null) {
            return tspInstance.getDimension();
        }

        Object dimObj = state.get(StateKeys.DIMENSION);
        if (dimObj instanceof Number dimension) {
            return dimension.intValue();
        }

        return 0;
    }

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
        if (tspInstance == null) {
            return 1.0;
        }

        double distance = tspInstance.getDistance(from, to);
        return Math.max(distance, 1e-9);
    }

    private void updatePheromoneMatrix(State state) {
        Object evaluatedObj = state.get(StateKeys.GENERATION_EVALUATED);
        if (!(evaluatedObj instanceof List<?> evaluated) || evaluated.isEmpty()) {
            return;
        }

        if (reinforcementMode == ReinforcementMode.BEST_SO_FAR) {
            updateBestSoFar(evaluated);

            if (bestSoFar == null) {
                return;
            }

            evaporate();
            depositPheromone(bestSoFar.value(), bestSoFar.fitness(), reinforcementRate);
        } else if (reinforcementMode == ReinforcementMode.ITERATION_BEST) {
            EvaluatedSolution<int[]> best = bestOf(evaluated);

            if (best == null) {
                return;
            }

            evaporate();
            depositPheromone(best.value(), best.fitness(), reinforcementRate);
        } else {
            evaporate();

            double scaledRate = reinforcementRate / evaluated.size();

            for (Object entry : evaluated) {
                if (entry instanceof EvaluatedSolution<?>(Object value, double fitness) && value instanceof int[] tour) {
                    depositPheromone(tour, fitness, scaledRate);
                }
            }
        }

        clampPheromones();
    }

    private void updateBestSoFar(List<?> evaluated) {
        EvaluatedSolution<int[]> generationBest = bestOf(evaluated);
        if (generationBest == null) {
            return;
        }

        if (bestSoFar == null || isAcceptedAsBestSoFar(generationBest)) {
            bestSoFar = new EvaluatedSolution<>(generationBest.value().clone(), generationBest.fitness());
        }
    }

    private boolean isAcceptedAsBestSoFar(EvaluatedSolution<int[]> candidate) {
        if (acceptEqualFitness) {
            return candidate.fitness() >= bestSoFar.fitness();
        }

        return candidate.fitness() > bestSoFar.fitness();
    }

    private EvaluatedSolution<int[]> bestOf(List<?> evaluated) {
        EvaluatedSolution<int[]> best = null;

        for (Object entry : evaluated) {
            if (entry instanceof EvaluatedSolution<?>(Object value, double fitness) && value instanceof int[] tour) {
                if (best == null || fitness > best.fitness()) {
                    best = new EvaluatedSolution<>(tour, fitness);
                }
            }
        }

        return best;
    }

    private void depositPheromone(int[] tour, double fitness, double rate) {
        double tourLength = -fitness;

        if (tourLength <= 0.0 || Double.isNaN(tourLength) || Double.isInfinite(tourLength)) {
            return;
        }

        double deposit = (rate * Q) / tourLength;

        for (int i = 0; i < tour.length; i++) {
            int from = tour[i];
            int to = tour[(i + 1) % tour.length];

            pheromoneMatrix[from][to] += deposit;
            pheromoneMatrix[to][from] += deposit;
        }
    }

    private void evaporate() {
        for (int i = 0; i < pheromoneMatrix.length; i++) {
            for (int j = 0; j < pheromoneMatrix.length; j++) {
                pheromoneMatrix[i][j] *= (1.0 - evaporationRate);
            }
        }
    }

    private ReinforcementMode parseReinforcementMode(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Reinforcement mode must be provided");
        }

        String normalized = value.toString().trim().toUpperCase();

        try {
            return ReinforcementMode.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid reinforcement mode: " + value, ex);
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