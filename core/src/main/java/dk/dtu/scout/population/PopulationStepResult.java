package dk.dtu.scout.population;

import dk.dtu.scout.logging.RunState;

import java.util.Map;

public record PopulationStepResult<S>(
        RunState<S> runState,
        int evaluationsDelta,
        Map<String, Object> sharedStateVariables
) {
}
