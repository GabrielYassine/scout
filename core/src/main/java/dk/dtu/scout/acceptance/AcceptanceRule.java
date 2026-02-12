package dk.dtu.scout.acceptance;

import dk.dtu.scout.Component;

import java.util.Random;

public interface AcceptanceRule extends Component {
    boolean accept(double currentFitness, double candidateFitness, int iteration, Random rng);
}
