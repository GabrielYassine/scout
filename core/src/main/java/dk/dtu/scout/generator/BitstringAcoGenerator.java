package dk.dtu.scout.generator;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.util.FormulaEvaluator;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static dk.dtu.scout.generator.ReinforcementMode.ALL;
import static dk.dtu.scout.generator.ReinforcementMode.BEST_SO_FAR;
import static dk.dtu.scout.generator.ReinforcementMode.ITERATION_BEST;

/**
 *
 * @author Ahmed
 */
@Component
@Scope("prototype")
public class BitstringAcoGenerator implements Generator<boolean[]> {

    private double evaporationRate = 0.1;
    private double reinforcementRate = 0.1;

    private Object minPheromone = "1/n";
    private Object maxPheromone = "1 - 1/n";

    private ReinforcementMode reinforcementMode = BEST_SO_FAR;
    private boolean acceptEqualFitness = true;

    private double[] pheromoneVector;
    private State state;
    private EvaluatedSolution<boolean[]> bestSoFar;

    @Override
    public void init(State state) {
        this.state = state;
    }

    @Override
    public String id() {
        return "bitstring-aco";
    }

    @Override
    public String displayName() {
        return "Bitstring ACO Generator";
    }

    @Override
    public String description() {
        return "ACO generator for bitstrings using bounded position pheromones";
    }

    @Override
    public List<Parameter> params() {
        return List.of(
            new Parameter("evaporationRate", "Pheromone Evaporation Rate", "double", evaporationRate, 0.0, 1.0, null),
            new Parameter("reinforcementRate", "Pheromone Reinforcement Rate", "double", reinforcementRate, 0.0, 1.0, null),
            new Parameter("minPheromone", "Minimum Pheromone", "string", minPheromone, null, null, null),
            new Parameter("maxPheromone", "Maximum Pheromone", "string", maxPheromone, null, null, null),
            new Parameter("reinforcementMode", "Reinforcement Mode", "enum", reinforcementMode.name(), null, null, List.of("BEST_SO_FAR", "ITERATION_BEST", "ALL")),
            new Parameter("acceptEqualFitness", "Accept Equal Fitness", "boolean", acceptEqualFitness, null, null, null)
        );
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params.containsKey("evaporationRate")) {
            evaporationRate = resolveRate(params.get("evaporationRate"), "Pheromone evaporation rate");
        }

        if (params.containsKey("reinforcementRate")) {
            reinforcementRate = resolveRate(params.get("reinforcementRate"), "Pheromone reinforcement rate");
        }

        if (params.containsKey("minPheromone")) {
            minPheromone = params.get("minPheromone");
        }

        if (params.containsKey("maxPheromone")) {
            maxPheromone = params.get("maxPheromone");
        }

        boolean hasReinforcementMode = params.containsKey("reinforcementMode");

        if (hasReinforcementMode) {
            reinforcementMode = parseReinforcementMode(params.get("reinforcementMode"));
        }

        if (params.containsKey("acceptEqualFitness")) {
            acceptEqualFitness = (Boolean) params.get("acceptEqualFitness");
        }
    }

    @Override
    public List<String> supportedSearchSpaces() {
        return List.of("bitstring");
    }

    @Override
    public boolean[] generate(Random rng) {
        if (pheromoneVector == null) {
            initializePheromones();
        }

        boolean[] result = new boolean[pheromoneVector.length];

        for (int i = 0; i < pheromoneVector.length; i++) {
            result[i] = rng.nextDouble() < pheromoneVector[i];
        }

        return result;
    }

    @Override
    public Map<String, Object> getStateVariables(State state) {
        if (pheromoneVector == null) {
            initializePheromones();
        }

        updatePheromones(state);
        state.update(Map.of(StateKeys.PHEROMONE_VECTOR, pheromoneVector));

        return Map.of(StateKeys.PHEROMONE_VECTOR, pheromoneVector);
    }

    private void initializePheromones() {
        int dimension = resolveDimension();
        PheromoneBounds bounds = resolvePheromoneBounds();
        double initialPheromone = clamp(0.5, bounds);

        pheromoneVector = new double[dimension];

        for (int i = 0; i < dimension; i++) {
            pheromoneVector[i] = initialPheromone;
        }

        state.update(Map.of(StateKeys.PHEROMONE_VECTOR, pheromoneVector));
    }

    private void updatePheromones(State state) {
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

    private void reinforceBestSoFar(List<?> evaluated) {
        updateBestSoFar(evaluated);
        evaporate();
        applyReinforcement(bestSoFar.value(), reinforcementRate);
    }

    private void reinforceIterationBest(List<?> evaluated) {
        EvaluatedSolution<boolean[]> generationBest = bestOf(evaluated);
        evaporate();
        applyReinforcement(generationBest.value(), reinforcementRate);
    }

    @SuppressWarnings("unchecked")
    private void reinforceAll(List<?> evaluated) {
        evaporate();

        double scaledRate = reinforcementRate / evaluated.size();

        for (Object entry : evaluated) {
            EvaluatedSolution<boolean[]> solution = (EvaluatedSolution<boolean[]>) entry;
            applyReinforcement(solution.value(), scaledRate);
        }
    }

    private void updateBestSoFar(List<?> evaluated) {
        EvaluatedSolution<boolean[]> generationBest = bestOf(evaluated);
        if (bestSoFar == null || isAcceptedAsBestSoFar(generationBest)) {
            bestSoFar = new EvaluatedSolution<>(generationBest.value().clone(), generationBest.fitness());
        }
    }

    private boolean isAcceptedAsBestSoFar(EvaluatedSolution<boolean[]> candidate) {
        return acceptEqualFitness ? candidate.fitness() >= bestSoFar.fitness() : candidate.fitness() > bestSoFar.fitness();
    }

    private void applyReinforcement(boolean[] reinforcedSolution, double rate) {
        for (int i = 0; i < pheromoneVector.length; i++) {
            if (reinforcedSolution[i]) {
                pheromoneVector[i] += rate;
            }
        }
    }

    private void evaporate() {
        for (int i = 0; i < pheromoneVector.length; i++) {
            pheromoneVector[i] *= (1.0 - evaporationRate);
        }
    }

    @SuppressWarnings("unchecked")
    private EvaluatedSolution<boolean[]> bestOf(List<?> evaluated) {
        EvaluatedSolution<boolean[]> best = null;

        for (Object entry : evaluated) {
            EvaluatedSolution<boolean[]> candidate = (EvaluatedSolution<boolean[]>) entry;

            if (best == null || candidate.fitness() > best.fitness()) {
                best = candidate;
            }
        }

        return best;
    }

    private double resolveRate(Object value, String label) {
        double resolved = ((Number) value).doubleValue();

        if (resolved < 0.0 || resolved > 1.0) {
            throw new IllegalArgumentException(label + " must be between 0 and 1");
        }

        return resolved;
    }

    private int resolveDimension() {
        return ((Number) state.get(StateKeys.DIMENSION)).intValue();
    }

    private PheromoneBounds resolvePheromoneBounds() {
        int dimension = resolveDimension();

        double resolvedMin = resolveFormulaOrNumber(minPheromone, dimension, "Minimum pheromone");
        double resolvedMax = resolveFormulaOrNumber(maxPheromone, dimension, "Maximum pheromone");

        validatePheromoneBounds(resolvedMin, resolvedMax);

        return new PheromoneBounds(resolvedMin, resolvedMax);
    }

    private double resolveFormulaOrNumber(Object value, int dimension, String label) {
        double resolved = switch (value) {
            case String formula -> FormulaEvaluator.eval(formula, dimension);
            case Number number -> number.doubleValue();
            default -> throw new IllegalArgumentException(label + " must be a number or formula");
        };

        if (!Double.isFinite(resolved)) {
            throw new IllegalArgumentException(label + " must resolve to a finite number");
        }

        return resolved;
    }

    private void validatePheromoneBounds(double min, double max) {
        if (min < 0.0 || min > 1.0) {
            throw new IllegalArgumentException("Minimum pheromone must be between 0 and 1");
        }

        if (max < 0.0 || max > 1.0) {
            throw new IllegalArgumentException("Maximum pheromone must be between 0 and 1");
        }

        if (min > max) {
            throw new IllegalArgumentException("Minimum pheromone cannot be greater than maximum pheromone");
        }
    }

    private void clampPheromones() {
        PheromoneBounds bounds = resolvePheromoneBounds();

        for (int i = 0; i < pheromoneVector.length; i++) {
            pheromoneVector[i] = clamp(pheromoneVector[i], bounds);
        }
    }

    private double clamp(double value, PheromoneBounds bounds) {
        return Math.max(bounds.min(), Math.min(bounds.max(), value));
    }

    private ReinforcementMode parseReinforcementMode(Object value) {
        String normalized = value.toString().trim().toUpperCase();

        try {
            return ReinforcementMode.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid reinforcement mode: " + value, ex);
        }
    }

    private record PheromoneBounds(double min, double max) {
    }
}