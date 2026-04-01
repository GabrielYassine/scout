package dk.dtu.scout;

/**
 * Canonical keys used in the shared per-run {@link State} map.
 *
 * <p>These constants reduce hidden coupling between components that exchange
 * values through the shared state blackboard.
 */
public final class StateKeys {

    private StateKeys() {
    }

    public static final String PROBLEM = "problem";
    public static final String DIMENSION = "dimension";
    public static final String SEARCH_SPACE_ID = "searchSpaceId";

    public static final String CURRENT = "current";
    public static final String CURRENT_FITNESS = "currentFitness";
    public static final String BEST = "best";
    public static final String BEST_FITNESS = "bestFitness";

    public static final String PARENTS_EVALUATED = "parentsEvaluated";
    public static final String GENERATION_EVALUATED = "generationEvaluated";

    public static final String OFFSPRING_BASE = "offspringBase";
    public static final String SELECTED_PARENT_1 = "selectedParent1";
    public static final String SELECTED_PARENT_2 = "selectedParent2";

    public static final String ISLAND_INDEX = "islandIndex";

    public static final String PHEROMONE_MATRIX = "pheromoneMatrix";
    public static final String PHEROMONE_VECTOR = "pheromoneVector";
}
