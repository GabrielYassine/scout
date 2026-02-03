package dk.dtu.scout.acceptance;

import java.util.Random;

public class ElitistAcceptance implements AcceptanceRule {
    @Override
    public boolean accept(double currentFitness, double candidateFitness, int iteration, Random rng) {
        return candidateFitness >= currentFitness;
    }
}
