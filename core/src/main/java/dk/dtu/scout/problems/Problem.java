package dk.dtu.scout.problems;

import dk.dtu.scout.Component;

import java.util.Random;

public interface Problem<S> extends Component  {
    S randomSolution(Random rng);
    double fitness(S solution);
    default boolean isOptimal(double fitness) { return true; }
}