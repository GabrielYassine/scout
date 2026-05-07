package dk.dtu.scout.selection;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.dto.Parameter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Selection rule based on simulated annealing.
 * @param <S> solution representation type
 * @author s230632
 */
@Component
@Scope("prototype")
public class AnnealedSelection<S> implements SelectionRule<S> {

    private double initialTemperature = 5.0;
    private double coolingRate = 0.995;
    private double minTemperature = 1e-6;

    private double currentTemperature = initialTemperature;

    public AnnealedSelection() {}

    @Override
    public String id() {
        return "annealed-selection";
    }

    @Override
    public String displayName() {
        return "Annealed Selection";
    }

    @Override
    public String description() {
        return "Classical SA/Metropolis for (1+1), temperature-based survivor selection for populations";
    }

    @Override
    public List<Parameter> params() {
        return List.of(
            new Parameter("initialTemperature", "Initial temperature (T0)", "double", initialTemperature, null, null, null),
            new Parameter("coolingRate", "Cooling rate", "double", coolingRate, null, null, null),
            new Parameter("minTemperature", "Min temperature", "double", minTemperature, null, null, null)
        );
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params.containsKey("initialTemperature")) {
            double value = ((Number) params.get("initialTemperature")).doubleValue();
            if (value <= 0.0) {
                throw new IllegalArgumentException("Initial temperature must be positive");
            }
            this.initialTemperature = value;
            this.currentTemperature = value;
        }

        if (params.containsKey("coolingRate")) {
            double value = ((Number) params.get("coolingRate")).doubleValue();
            if (value <= 0.0 || value > 1.0) {
                throw new IllegalArgumentException("Cooling rate must be in the interval (0, 1]");
            }
            this.coolingRate = value;
        }

        if (params.containsKey("minTemperature")) {
            double value = ((Number) params.get("minTemperature")).doubleValue();
            if (value <= 0.0) {
                throw new IllegalArgumentException("Minimum temperature must be positive");
            }
            this.minTemperature = value;
        }

        if (minTemperature > initialTemperature) {
            throw new IllegalArgumentException("Minimum temperature cannot be greater than initial temperature");
        }
    }

    /**
     * Calculates the temperature at a given iteration using exponential cooling schedule.
     * @param iteration the current iteration number (0-based)
     * @return the temperature for the given iteration, not going below minTemperature
     */
    public double temperatureAt(int iteration) {
        int safeIteration = Math.max(0, iteration);
        double temperature = initialTemperature * Math.pow(coolingRate, safeIteration);
        return Math.max(minTemperature, temperature);
    }
    /**
     * Returns the current temperature as a state variable for monitoring purposes.
     * @param state the current state of the optimization process
     * @return a map containing the current temperature under the key StateKeys.TEMPERATURE
    */
    @Override
    public Map<String, Object> getStateVariables(State state) {
        return Map.of(StateKeys.TEMPERATURE, currentTemperature);
    }
    /**
     * Selects the next parent population using annealed survivor selection.
     * @param parents current evaluated parent population
     * @param children evaluated offspring population
     * @param mu number of parents to select for the next generation
     * @param iteration current iteration number
     * @param rng random number generator
     * @return selected parent population for the next generation
     */
    @Override
    public List<EvaluatedSolution<S>> select(
        List<EvaluatedSolution<S>> parents,
        List<EvaluatedSolution<S>> children,
        int mu,
        int iteration,
        Random rng
    ) {
        if (mu <= 0) {
            throw new IllegalArgumentException("mu must be positive");
        }

        if (parents == null || parents.isEmpty()) {
            throw new IllegalArgumentException("AnnealedSelection requires at least one parent");
        }

        if (children == null || children.isEmpty()) {
            throw new IllegalArgumentException("AnnealedSelection requires at least one child");
        }

        currentTemperature = temperatureAt(iteration);

        if (mu == 1 && parents.size() == 1 && children.size() == 1) {
            return selectClassicalAnnealing(parents.getFirst(), children.getFirst(), currentTemperature, rng);
        }

        return selectPopulationAnnealed(parents, children, mu, currentTemperature, rng);
    }
    /**
     * Applies the classical simulated annealing acceptance rule.
     * Improving candidates are always accepted. Worsening candidates are accepted
     * with probability exp(delta / temperature), where delta is the fitness
     * difference between candidate and current solution.
     * @param current current parent solution
     * @param candidate candidate child solution
     * @param temperature current temperature
     * @param rng random number generator
     * @return list containing the accepted solution
     */
    private List<EvaluatedSolution<S>> selectClassicalAnnealing(
        EvaluatedSolution<S> current,
        EvaluatedSolution<S> candidate,
        double temperature,
        Random rng
    ) {
        double delta = candidate.fitness() - current.fitness();
        boolean accept = delta >= 0.0 || rng.nextDouble() < Math.exp(delta / temperature);
        return List.of(accept ? candidate : current);
    }
    /**
     * Performs annealed selection for population-based runs.
     * Parents and children are combined into one candidate pool. Candidates are then
     * sampled without replacement using temperature-weighted fitness probabilities.
     * @param parents current evaluated parent population
     * @param children evaluated offspring population
     * @param mu number of candidates to select
     * @param temperature current temperature
     * @param rng random number generator
     * @return selected parent population
     */
    private List<EvaluatedSolution<S>> selectPopulationAnnealed(
        List<EvaluatedSolution<S>> parents,
        List<EvaluatedSolution<S>> children,
        int mu,
        double temperature,
        Random rng
    ) {
        List<EvaluatedSolution<S>> remaining = new ArrayList<>(parents.size() + children.size());
        remaining.addAll(parents);
        remaining.addAll(children);

        if (remaining.size() < mu) {
            throw new IllegalArgumentException("Annealed population selection requires at least mu candidates. Got mu=" + mu + " but only " + remaining.size() + " candidates are available.");
        }

        List<EvaluatedSolution<S>> selected = new ArrayList<>(mu);

        while (selected.size() < mu) {
            int selectedIndex = sampleByTemperatureWeightedFitness(remaining, temperature, rng);
            selected.add(remaining.remove(selectedIndex));
        }

        return selected;
    }
    /**
    * Samples a candidate index from the given list of candidates using temperature-weighted fitness probabilities.
    * The fitness values are shifted by the best fitness to ensure numerical stability when computing weights.
    * If all candidates have non-positive weights, a random candidate is selected uniformly.
     * @param candidates candidates to sample from
     * @param temperature current temperature
     * @param rng random number generator
     * @return selected candidate index
     */
    private int sampleByTemperatureWeightedFitness(List<EvaluatedSolution<S>> candidates, double temperature, Random rng) {
        double bestFitness = Double.NEGATIVE_INFINITY;

        for (EvaluatedSolution<S> candidate : candidates) {
            if (candidate.fitness() > bestFitness) {
                bestFitness = candidate.fitness();
            }
        }

        double[] weights = new double[candidates.size()];
        double totalWeight = 0.0;

        for (int i = 0; i < candidates.size(); i++) {
            double shiftedFitness = candidates.get(i).fitness() - bestFitness;
            double weight = Math.exp(shiftedFitness / temperature);

            if (Double.isNaN(weight)) {
                weight = 0.0;
            }

            weights[i] = weight;
            totalWeight += weight;
        }

        if (totalWeight <= 0.0) {
            return rng.nextInt(candidates.size());
        }

        double threshold = rng.nextDouble() * totalWeight;
        double cumulative = 0.0;

        for (int i = 0; i < weights.length - 1; i++) {
            cumulative += weights[i];
            if (threshold <= cumulative) {
                return i;
            }
        }

        return weights.length - 1;
    }
}