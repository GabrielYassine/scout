package dk.dtu.scout.acceptance;

import java.util.Objects;
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
