package dk.dtu.scout.generator;

import dk.dtu.scout.EvaluatedSolution;
import dk.dtu.scout.Parameter;
import dk.dtu.scout.State;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
@Scope("prototype")
public class BitstringAcoGenerator extends AbstractAcoGenerator<boolean[]> {

    private static final double Q = 0.1;
    private static final double MIN_PHEROMONE = 0.01;
    private static final double MAX_PHEROMONE = 0.99;

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
        return acoParams();
    }

    @Override
    public boolean[] generate(Random rng) {
        if (pheromoneVector == null) {
            initializePheromones();
        }

        int n = pheromoneVector.length;
        boolean[] result = new boolean[n];

        for (int i = 0; i < n; i++) {
            double p1 = Math.pow(pheromoneVector[i], alpha);
            double p0 = Math.pow(1.0 - pheromoneVector[i], beta);
            double probabilityOfOne = p1 / (p1 + p0);
            result[i] = rng.nextDouble() < probabilityOfOne;
        }

        return result;
    }

    @Override
    public Map<String, Object> getStateVariables(State state) {
        if (pheromoneVector == null) {
            initializePheromones();
        }
        updatePheromones(state);
        return Map.of("pheromoneVector", pheromoneVector);
    }

    private void initializePheromones() {
        int n = 0;
        if (state != null) {
            Object dimObj = state.get("dimension");
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
            state.update(Map.of("pheromoneVector", pheromoneVector));
        }
    }

    private void updatePheromones(State state) {
        if (state == null || pheromoneVector == null || pheromoneVector.length == 0) {
            return;
        }

        Object evaluatedObj = state.get("generationEvaluated");
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
            pheromoneVector[i] = clamp(pheromoneVector[i] + Q * (target - pheromoneVector[i]));
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
