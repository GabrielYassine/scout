package dk.dtu.scout.acceptance;

import dk.dtu.scout.EvaluatedSolution;
import dk.dtu.scout.Parameter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


import java.util.*;

/**
 * Simulated Annealing acceptance rule for optimization algorithms.
 * Accepts improvements always, and accepts worse moves with probability based on temperature.
 */

@Component
@Scope("prototype")
public class AnnealedSelection<S> implements SelectionRule<S> {

    private double initialTemperature = 5.0;
    private double coolingRate = 0.995;
    private double minTemperature = 1e-6;

    public AnnealedSelection() {}

    @Override
    public String id() {
        return "simulated-annealing";
    }

    @Override
    public String displayName() {
        return "Simulated Annealing Acceptance";
    }

    @Override
    public String description() {
        return "Accepts worse solutions with a probability that decreases over time";
    }

    @Override
    public List<Parameter> params() {
        return List.of(
            new Parameter("initialTemperature", "Initial temperature (T0)", "double", initialTemperature, null, null),
            new Parameter("coolingRate", "Cooling rate", "double", coolingRate, null, null),
            new Parameter("minTemperature", "Min temperature", "double", minTemperature, null, null)
        );
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) return;
        this.initialTemperature = ((Number) params.getOrDefault("initialTemperature", initialTemperature)).doubleValue();
        this.coolingRate = ((Number) params.getOrDefault("coolingRate", coolingRate)).doubleValue();
        this.minTemperature = ((Number) params.getOrDefault("minTemperature", minTemperature)).doubleValue();
    }

    /**  cooling: T = max(Tmin, T0 * coolingRate^iteration) */
    /** Look at the formal again*/
    public double temperatureAt(int iteration) {
        double t = initialTemperature * Math.pow(coolingRate, iteration);
        return Math.max(minTemperature, t);
    }

    @Override
    public List<EvaluatedSolution<S>> select(
            List<EvaluatedSolution<S>> parents,
            List<EvaluatedSolution<S>> children,
            int mu,
            int iteration,
            Random rng
    ) {
        List<EvaluatedSolution<S>> pool = new ArrayList<>(parents.size() + children.size());
        pool.addAll(parents);
        pool.addAll(children);

        if (pool.isEmpty()) {
            return List.of();
        }

        // Sort best-to-worst so we have a reference best candidate
        pool.sort(Comparator.comparingDouble(EvaluatedSolution<S>::fitness).reversed());

        List<EvaluatedSolution<S>> selected = new ArrayList<>(mu);
        double temperature = temperatureAt(iteration);

        while (selected.size() < mu && !pool.isEmpty()) {
            EvaluatedSolution<S> bestCandidate = pool.getFirst();
            boolean picked = false;

            for (int i = 0; i < pool.size(); i++) {
                EvaluatedSolution<S> candidate = pool.get(i);

                if (candidate.fitness() >= bestCandidate.fitness()) {
                    selected.add(candidate);
                    pool.remove(i);
                    picked = true;
                    break;
                }

                double delta = candidate.fitness() - bestCandidate.fitness();
                double probability = Math.exp(delta / temperature);

                if (rng.nextDouble() < probability) {
                    selected.add(candidate);
                    pool.remove(i);
                    picked = true;
                    break;
                }
            }

            // fallback: if nothing was accepted, take the current best
            if (!picked) {
                selected.add(pool.removeFirst());
            }
        }

        // keep result sorted best-to-worst for consistency
        selected.sort(Comparator.comparingDouble(EvaluatedSolution<S>::fitness).reversed());
        return selected;
    }
}
