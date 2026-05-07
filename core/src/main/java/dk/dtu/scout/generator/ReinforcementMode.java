package dk.dtu.scout.generator;


/**
 * Defines how an ACO generator chooses which evaluated solutions should
 * reinforce the pheromone values.
 * @author s230632 & s235257
 */
public enum ReinforcementMode {
    BEST_SO_FAR,
    ITERATION_BEST,
    ALL
}
