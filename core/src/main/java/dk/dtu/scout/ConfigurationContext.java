package dk.dtu.scout;

import dk.dtu.scout.problems.Problem;

/**
 * Context object that provides runtime information needed during component configuration.
 * This allows components to access dimension, problem references, etc. without hardcoding
 * special cases in services.
 */
public class ConfigurationContext {
    private final int dimension;
    private final Problem<?> problem; // Optional - only needed for observers and stop conditions

    /**
     * Create context with just dimension (for mutations, search spaces, etc.)
     */
    public ConfigurationContext(int dimension) {
        this(dimension, null);
    }

    /**
     * Create context with dimension and problem reference (for observers, stop conditions)
     */
    public ConfigurationContext(int dimension, Problem<?> problem) {
        this.dimension = dimension;
        this.problem = problem;
    }

    public int getDimension() {
        return dimension;
    }

    public Problem<?> getProblem() {
        return problem;
    }

    public boolean hasProblem() {
        return problem != null;
    }
}
