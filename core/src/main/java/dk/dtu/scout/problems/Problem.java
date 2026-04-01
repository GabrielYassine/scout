package dk.dtu.scout.problems;

import dk.dtu.scout.ScoutComponent;

import java.util.Map;

/**
 * Optimization problem contract mapping solutions to fitness values.
 */
public interface Problem<S> extends ScoutComponent {
    double fitness(S solution);
    default boolean isOptimal(double fitness) { return true; }
    default void configure(Map<String, Object> params) {}
}