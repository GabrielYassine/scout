package dk.dtu.scout.searchSpace;

import java.util.Random;

/**
 * Represents a search space for optimization algorithms.
 * Defines the structure and operations available for solutions in this space.
 * @param <S> The solution type (e.g., boolean[] for bitstrings)
 */

public interface SearchSpace<S> {
    S randomSolution(Random rng);
}