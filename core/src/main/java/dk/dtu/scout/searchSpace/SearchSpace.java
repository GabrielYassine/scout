package dk.dtu.scout.searchSpace;

import dk.dtu.scout.ScoutComponent;

import java.util.Random;

/**
 * Defines solution representation and random sampling for a search space.
 */

public interface SearchSpace<S> extends ScoutComponent {
    S randomSolution(Random rng);
    int dimension();
}