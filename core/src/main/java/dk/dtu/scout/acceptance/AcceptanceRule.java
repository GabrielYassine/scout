package dk.dtu.scout.acceptance;

import java.util.Random;

public interface AcceptanceRule {
    boolean accept(double currentFitness, double candidateFitness, int iteration, Random rng);
}
