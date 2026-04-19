package dk.dtu.scout.population;

import dk.dtu.scout.ScoutComponent;

import java.util.List;
import java.util.Map;

public record PopulationInitialization<S>(
        PopulationState<S> state,
        IterationSnapshot<S> initialState,
        int evaluations,
        Map<String, Object> sharedStateVariables,
        List<ScoutComponent> stateComponents
) {
}
