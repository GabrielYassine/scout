package dk.dtu.scout.generator;

import dk.dtu.scout.EvaluatedSolution;
import dk.dtu.scout.Parameter;
import dk.dtu.scout.State;
import dk.dtu.scout.StateKeys;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
@Scope("prototype")
public class BitstringAcoGenerator extends AbstractAcoGenerator<boolean[]> {

    private static final double MIN_PHEROMONE = 0.01;
    private static final double MAX_PHEROMONE = 0.99;

    private double reinforcementRate = 0.1;

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
        return "Ant colony generator for bitstrings using position pheromones";
    }

    @Override
    public List<Parameter> params() {
        return List.of(
                new Parameter("evaporationRate", "Pheromone Evaporation Rate", "double", evaporationRate, 0.0, 1.0),
                new Parameter("reinforcementRate", "Reinforcement Rate", "double", reinforcementRate, 0.0, 1.0)
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
    }

    @Override
    public boolean[] generate(Random rng) {
        if (pheromoneVector == null) {
            initializePheromones();
        }

        int n = pheromoneVector.length;
        boolean[] result = new boolean[n];

        for (int i = 0; i < n; i++) {
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
        return Map.of(StateKeys.PHEROMONE_VECTOR, pheromoneVector);
    }

    private void initializePheromones() {
        int n = 0;
        if (state != null) {
            Object dimObj = state.get(StateKeys.DIMENSION);
            if (dimObj instanceof Integer) {
                n = (Integer) dimObj;
            }
            if (n <= 0) {
                Object nObj = state.get("n");
                if (nObj instanceof Integer) {
                    n = (Integer) nObj;
                }
            }
        }

        if (n <= 0) {
            throw new IllegalStateException("Cannot initialize BitstringAcoGenerator: dimension must be positive. Got dimension=" + n + " from state=" + state);
        }

        pheromoneVector = new double[n];
        for (int i = 0; i < n; i++) {
            pheromoneVector[i] = 0.5;
        }

        if (state != null) {
            state.update(Map.of(StateKeys.PHEROMONE_VECTOR, pheromoneVector));
        }
    }

    private void updatePheromones(State state) {
        if (state == null || pheromoneVector == null || pheromoneVector.length == 0) {
            return;
        }

        Object evaluatedObj = state.get(StateKeys.GENERATION_EVALUATED);
        if (!(evaluatedObj instanceof List<?> evaluated)) {
            return;
        }
        if (evaluated.isEmpty()) {
            return;
        }

        evaporate(evaporationRate);

        EvaluatedSolution<boolean[]> best = bestOf(evaluated);
        if (best != null) {
            deposit(best.value());
        }
    }

    private void evaporate(double rate) {
        for (int i = 0; i < pheromoneVector.length; i++) {
            double updated = (1.0 - rate) * pheromoneVector[i] + rate * 0.5;
            pheromoneVector[i] = clamp(updated);
        }
    }

    private void deposit(boolean[] solution) {
        for (int i = 0; i < pheromoneVector.length; i++) {
            double target = solution[i] ? 1.0 : 0.0;
            pheromoneVector[i] = clamp(pheromoneVector[i] + reinforcementRate * (target - pheromoneVector[i]));
        }
    }

    private EvaluatedSolution<boolean[]> bestOf(List<?> evaluated) {
        EvaluatedSolution<boolean[]> best = null;
        for (Object entry : evaluated) {
            if (entry instanceof EvaluatedSolution<?>(Object value, double fitness) && value instanceof boolean[]) {
                if (best == null || fitness > best.fitness()) {
                    best = new EvaluatedSolution<>((boolean[]) value, fitness);
                }
            }
        }
        return best;
    }

    private double clamp(double value) {
        if (value < MIN_PHEROMONE) {
            return MIN_PHEROMONE;
        }
        if (value > MAX_PHEROMONE) {
            return MAX_PHEROMONE;
        }
        return value;
    }
}
