package dk.dtu.scout.problems;

import java.util.Random;

public interface Problem<S> {
    S randomSolution(Random rng);
    double fitness(S solution);
    default boolean isOptimal(double fitness) { return true; }
}