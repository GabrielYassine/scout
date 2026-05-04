package dk.dtu.scout.population;

import dk.dtu.scout.ScoutComponent;
import dk.dtu.scout.logging.IterationSnapshot;

import java.util.List;
import java.util.Map;

public record PopulationInitialization<S>(
        PopulationState<S> state,
        IterationSnapshot<S> initialState,
        int evaluations,
        Map<String, Object> sharedStateVariables,
        List<ScoutComponent> stateComponents
) {
    public PopulationInitialization {
        sharedStateVariables = sharedStateVariables != null ? sharedStateVariables : Map.of();
        stateComponents = stateComponents != null ? stateComponents : List.of();
    }
}