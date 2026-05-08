package dk.dtu.scout.crossover;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;

import java.util.Map;

public final class CrossoverTestSupport {

    private CrossoverTestSupport() {
    }

    public static State stateWithParents(boolean[] parent1, boolean[] parent2) {
        State state = new State();
        state.update(Map.of(
            StateKeys.SELECTED_PARENT_1, parent1,
            StateKeys.SELECTED_PARENT_2, parent2
        ));
        return state;
    }
}