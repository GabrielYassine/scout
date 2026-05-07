package dk.dtu.scout.crossover;

import dk.dtu.scout.ScoutComponent;

import java.util.Random;

/**
 * Defines a crossover operator that combines selected parent solutions
 * into a new offspring solution.
 *
 * @param <S> the solution representation type
 * @author Ahmed
 */
public interface Crossover<S> extends ScoutComponent {
    S crossover(Random rng);
}