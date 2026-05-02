package dk.dtu.scout.generator;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.dto.Parameter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
@Scope("prototype")
public class BitstringAcoGenerator implements Generator<boolean[]> {

    private static final String REINFORCEMENT_BEST = "best";
    private static final String REINFORCEMENT_ALL = "all";

    private double evaporationRate = 0.1;
    private double reinforcementRate = 0.1;

    private double minPheromone = 0.01;
    private double maxPheromone = 0.99;

    private String reinforcementMode = REINFORCEMENT_BEST;

    private double[] pheromoneVector;
    private State state;

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
        return "Ant colony generator for bitstrings using bounded position pheromones";
    }

    @Override
    public List<Parameter> params() {
        return List.of(
                new Parameter("evaporationRate", "Pheromone Evaporation Rate", "double", evaporationRate, 0.0, 1.0),
                new Parameter("reinforcementRate", "Reinforcement Rate", "double", reinforcementRate, 0.0, 1.0),
                new Parameter("minPheromone", "Minimum Pheromone", "double", minPheromone, 0.0, 1.0),
                new Parameter("maxPheromone", "Maximum Pheromone", "double", maxPheromone, 0.0, 1.0),
                new Parameter("reinforcementMode", "Reinforcement Mode (best/all)", "string", reinforcementMode, null, null)
        );
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) {
            return;
        }

        if (params.containsKey("evaporationRate")) {
            double value = ((Number) params.get("evaporationRate")).doubleValue();
            if (value < 0.0 || value > 1.0) {
                throw new IllegalArgumentException("Evaporation rate must be between 0 and 1");
            }
            this.evaporationRate = value;
        }

        if (params.containsKey("reinforcementRate")) {
            double value = ((Number) params.get("reinforcementRate")).doubleValue();
            if (value < 0.0 || value > 1.0) {
                throw new IllegalArgumentException("Reinforcement rate must be between 0 and 1");
            }
            this.reinforcementRate = value;
        }

        if (params.containsKey("minPheromone")) {
            double value = ((Number) params.get("minPheromone")).doubleValue();
            if (value < 0.0 || value > 1.0) {
                throw new IllegalArgumentException("Minimum pheromone must be between 0 and 1");
            }
            this.minPheromone = value;
        }

        if (params.containsKey("maxPheromone")) {
            double value = ((Number) params.get("maxPheromone")).doubleValue();
            if (value < 0.0 || value > 1.0) {
                throw new IllegalArgumentException("Maximum pheromone must be between 0 and 1");
            }
            this.maxPheromone = value;
        }

        if (params.containsKey("reinforcementMode")) {
            String value = String.valueOf(params.get("reinforcementMode")).trim().toLowerCase();
            if (!REINFORCEMENT_BEST.equals(value) && !REINFORCEMENT_ALL.equals(value)) {
                throw new IllegalArgumentException("Reinforcement mode must be either 'best' or 'all'");
            }
            this.reinforcementMode = value;
        }

        if (minPheromone > maxPheromone) {
            throw new IllegalArgumentException("Minimum pheromone cannot be greater than maximum pheromone");
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

        if (this.state != null) {
            this.state.update(Map.of(StateKeys.PHEROMONE_VECTOR, pheromoneVector));
        }

        return Map.of(StateKeys.PHEROMONE_VECTOR, pheromoneVector);
    }

    private void initializePheromones() {
        int dimension = resolveDimension();

        if (dimension <= 0) {
            throw new IllegalStateException("Cannot initialize BitstringAcoGenerator: dimension must be positive");
        }

        double initialPheromone = clamp(0.5);

        pheromoneVector = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            pheromoneVector[i] = initialPheromone;
        }

        if (state != null) {
            state.update(Map.of(StateKeys.PHEROMONE_VECTOR, pheromoneVector));
        }
    }

    private int resolveDimension() {
        if (state != null) {
            Object dimObj = state.get(StateKeys.DIMENSION);
            if (dimObj instanceof Number dimension) {
                return dimension.intValue();
            }

            Object nObj = state.get("n");
            if (nObj instanceof Number n) {
                return n.intValue();
            }
        }

        return 0;
    }

    private void updatePheromones(State state) {
        if (state == null || pheromoneVector == null || pheromoneVector.length == 0) {
            return;
        }

        Object evaluatedObj = state.get(StateKeys.GENERATION_EVALUATED);
        if (!(evaluatedObj instanceof List<?> evaluated) || evaluated.isEmpty()) {
            return;
        }

        evaporate();

        if (REINFORCEMENT_BEST.equals(reinforcementMode)) {
            reinforceBest(evaluated);
        } else if (REINFORCEMENT_ALL.equals(reinforcementMode)) {
            reinforceAll(evaluated);
        }

        clampPheromones();
    }
    
    private void evaporate() {
        for (int i = 0; i < pheromoneVector.length; i++) {
            pheromoneVector[i] *= (1.0 - evaporationRate);
        }
    }

    private void reinforceBest(List<?> evaluated) {
        EvaluatedSolution<boolean[]> best = bestOf(evaluated);
        if (best != null) {
            deposit(best.value(), reinforcementRate);
        }
    }

    private void reinforceAll(List<?> evaluated) {
        double scaledReinforcement = reinforcementRate / evaluated.size();

        for (Object entry : evaluated) {
            if (entry instanceof EvaluatedSolution<?>(Object value, double fitness) && value instanceof boolean[] bits) {
                deposit(bits, scaledReinforcement);
            }
        }
    }

    private void deposit(boolean[] solution, double amount) {
        int limit = Math.min(pheromoneVector.length, solution.length);

        for (int i = 0; i < limit; i++) {
            if (solution[i]) {
                pheromoneVector[i] += amount;
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

    private void clampPheromones() {
        for (int i = 0; i < pheromoneVector.length; i++) {
            pheromoneVector[i] = clamp(pheromoneVector[i]);
        }
    }

    private double clamp(double value) {
        return Math.max(minPheromone, Math.min(maxPheromone, value));
    }
}