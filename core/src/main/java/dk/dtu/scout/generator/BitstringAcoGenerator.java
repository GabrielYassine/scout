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

@Component
@Scope("prototype")
public class BitstringAcoGenerator implements Generator<boolean[]> {

    private double rho = 0.1;

    private Object minPheromone = "1/n";
    private Object maxPheromone = "1 - 1/n";

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
        return "Bitstring MMAS Generator";
    }

    @Override
    public String description() {
        return "MMAS generator for bitstrings using bounded position pheromones";
    }

    @Override
    public List<Parameter> params() {
        return List.of(
                new Parameter("rho", "Pheromone Update Strength", "double", rho, 0.0, 1.0),
                new Parameter("minPheromone", "Minimum Pheromone", "string", minPheromone, null, null),
                new Parameter("maxPheromone", "Maximum Pheromone", "string", maxPheromone, null, null),
                new Parameter("acceptEqualFitness", "Accept Equal Fitness", "boolean", acceptEqualFitness, null, null)
        );
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params.containsKey("rho")) {
            this.rho = resolveRate(params.get("rho"), "Pheromone update strength");
        }

        // Compatibility with older saved configurations.
        if (!params.containsKey("rho") && params.containsKey("evaporationRate")) {
            this.rho = resolveRate(params.get("evaporationRate"), "Evaporation rate");
        }

        if (!params.containsKey("rho") && params.containsKey("reinforcementRate")) {
            this.rho = resolveRate(params.get("reinforcementRate"), "Reinforcement rate");
        }

        if (params.containsKey("evaporationRate") && params.containsKey("reinforcementRate")) {
            double evaporationRate = resolveRate(params.get("evaporationRate"), "Evaporation rate");
            double reinforcementRate = resolveRate(params.get("reinforcementRate"), "Reinforcement rate");

            if (Double.compare(evaporationRate, reinforcementRate) != 0) {
                throw new IllegalArgumentException(
                        "Bitstring MMAS uses one update strength rho. Evaporation and reinforcement must be equal."
                );
            }

            this.rho = evaporationRate;
        }

        if (params.containsKey("minPheromone")) {
            this.minPheromone = params.get("minPheromone");
        }

        if (params.containsKey("maxPheromone")) {
            this.maxPheromone = params.get("maxPheromone");
        }

        if (params.containsKey("acceptEqualFitness")) {
            this.acceptEqualFitness = (Boolean) params.get("acceptEqualFitness");
        }
    }

    private double resolveRate(Object value, String label) {
        double resolved = ((Number) value).doubleValue();

        if (resolved < 0.0 || resolved > 1.0) {
            throw new IllegalArgumentException(label + " must be between 0 and 1");
        }

        return resolved;
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

        if (dimension <= 0) {
            throw new IllegalStateException("Cannot initialize BitstringAcoGenerator: dimension must be positive");
        }

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

        updateBestSoFar(evaluated);

        if (bestSoFar == null) {
            return;
        }

        applyMmasUpdate(bestSoFar.value());

        clampPheromones();
    }

    private void updateBestSoFar(List<?> evaluated) {
        EvaluatedSolution<boolean[]> generationBest = bestOf(evaluated);

        if (generationBest == null) {
            return;
        }

        if (bestSoFar == null || isAcceptedAsBestSoFar(generationBest)) {
            bestSoFar = new EvaluatedSolution<>(
                    generationBest.value().clone(),
                    generationBest.fitness()
            );
        }
    }

    private boolean isAcceptedAsBestSoFar(EvaluatedSolution<boolean[]> candidate) {
        if (acceptEqualFitness) {
            return candidate.fitness() >= bestSoFar.fitness();
        }

        return candidate.fitness() > bestSoFar.fitness();
    }

    private void applyMmasUpdate(boolean[] reinforcedSolution) {
        int limit = Math.min(pheromoneVector.length, reinforcedSolution.length);

        for (int i = 0; i < pheromoneVector.length; i++) {
            pheromoneVector[i] *= (1.0 - rho);
        }

        for (int i = 0; i < limit; i++) {
            if (reinforcedSolution[i]) {
                pheromoneVector[i] += rho;
            }
        }
    }

    private EvaluatedSolution<boolean[]> bestOf(List<?> evaluated) {
        EvaluatedSolution<boolean[]> best = null;

        for (Object entry : evaluated) {
            if (entry instanceof EvaluatedSolution<?>(Object value, double fitness) && value instanceof boolean[] bits) {
                if (best == null || fitness > best.fitness()) {
                    best = new EvaluatedSolution<>(bits, fitness);
                }
            }
        }

        return best;
    }

    private PheromoneBounds resolvePheromoneBounds() {
        int dimension = resolveDimension();

        double resolvedMin = resolveFormulaOrNumber(minPheromone, dimension, "Minimum pheromone");
        double resolvedMax = resolveFormulaOrNumber(maxPheromone, dimension, "Maximum pheromone");

        validatePheromoneBounds(resolvedMin, resolvedMax);

        return new PheromoneBounds(resolvedMin, resolvedMax);
    }

    private double resolveFormulaOrNumber(Object value, int dimension, String label) {
        double resolved;

        if (value instanceof String formula) {
            resolved = FormulaEvaluator.eval(formula, dimension);
        } else if (value instanceof Number number) {
            resolved = number.doubleValue();
        } else {
            throw new IllegalArgumentException(label + " must be a number or formula");
        }

        if (Double.isNaN(resolved) || Double.isInfinite(resolved)) {
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

    private int resolveDimension() {
        Object dimObj = state.get(StateKeys.DIMENSION);
        if (dimObj instanceof Number dimension) {
            return dimension.intValue();
        }

        return 0;
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

    private record PheromoneBounds(double min, double max) {}
}