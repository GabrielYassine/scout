package dk.dtu.scout.crossover;

import dk.dtu.scout.ScoutComponent;

import java.util.Random;

/**
 * Combines parent solutions into a new offspring solution.
 * @author Ahmed
 */
public interface Crossover<S> extends ScoutComponent {
    S crossover(Random rng);
}