package dk.dtu.scout.acceptance;

import dk.dtu.scout.Parameter;

import java.util.List;
import java.util.Random;

/**
 * Simulated Annealing acceptance rule for optimization algorithms.
 * Accepts improvements always, and accepts worse moves with probability based on temperature.
 */
public class SimulatedAnnealingAcceptance implements AcceptanceRule {

    private final double initialTemperature;
    private final double coolingRate;
    private final double minTemperature;

    public SimulatedAnnealingAcceptance(double initialTemperature, double coolingRate, double minTemperature) {
        this.initialTemperature = initialTemperature;
        this.coolingRate = coolingRate;
        this.minTemperature = minTemperature;
    }

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
            new Parameter("initialTemperature", "Initial temperature (T0)", "double", 5.0, null, null),
            new Parameter("coolingRate", "Cooling rate", "double", 0.995, null, null),
            new Parameter("minTemperature", "Min temperature", "double", 1e-6, null, null)
        );
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
