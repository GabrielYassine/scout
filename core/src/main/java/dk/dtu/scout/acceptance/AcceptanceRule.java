package dk.dtu.scout.acceptance;

import dk.dtu.scout.ScoutComponent;

import java.util.Map;
import java.util.Random;

public interface AcceptanceRule extends ScoutComponent {
    boolean accept(double currentFitness, double candidateFitness, int iteration, Random rng);
    default void configure(Map<String, Object> params) {}
}
