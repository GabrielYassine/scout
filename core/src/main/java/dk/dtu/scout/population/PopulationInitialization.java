package dk.dtu.scout.population;

import dk.dtu.scout.ScoutComponent;
import dk.dtu.scout.logging.RunState;

import java.util.List;
import java.util.Map;

public record PopulationInitialization<S>(
        PopulationState<S> state,
        RunState<S> initialState,
        int evaluations,
        Map<String, Object> sharedStateVariables,
        List<ScoutComponent> stateComponents
) {
}
