package dk.dtu.scout.acceptance;

import dk.dtu.scout.Parameter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Simulated Annealing acceptance rule for optimization algorithms.
 * Accepts improvements always, and accepts worse moves with probability based on temperature.
 */

@Component
@Scope("prototype")
public class SimulatedAnnealingAcceptance implements AcceptanceRule {

    private double initialTemperature = 5.0;
    private double coolingRate = 0.995;
    private double minTemperature = 1e-6;

    public SimulatedAnnealingAcceptance() {}

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
    public boolean accept(double currentFitness, double candidateFitness, int iteration, Random rng) {
        if (candidateFitness >= currentFitness) return true;

        double T = temperatureAt(iteration);
        double delta = candidateFitness - currentFitness;
        // Probability of acceptance
        double p = Math.exp(delta / T);

        return rng.nextDouble() < p;
    }
}
