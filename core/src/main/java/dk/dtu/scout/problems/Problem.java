package dk.dtu.scout.problems;

import dk.dtu.scout.ScoutComponent;

/**
 * Optimization problem contract mapping solutions to fitness values.
 * @author s235257 & s230632
 */
public interface Problem<S> extends ScoutComponent {
    double fitness(S solution);

    /**
     * Returns whether the given fitness value is known to be optimal for this problem.
     * <p>The default is false because not every problem has a known optimum.
     * Problems with known optima, such as OneMax, LeadingOnes, TSP instances with
     * stored optima, or VRP instances with stored optima, should override this method.
     */
    boolean isOptimal(double fitness);
}