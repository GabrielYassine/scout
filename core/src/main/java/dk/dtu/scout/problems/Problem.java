package dk.dtu.scout.problems;

import dk.dtu.scout.ScoutComponent;

/**
 * Optimization problem contract mapping solutions to fitness values.
 * @author s235257 & s230632
 */
public interface Problem<S> extends ScoutComponent {
    double fitness(S solution);
    boolean isOptimal(double fitness);
}