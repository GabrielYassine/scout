package dk.dtu.scout.population;

import dk.dtu.scout.logging.IterationSnapshot;

import java.util.Map;

/**
 *
 * @param <S>
 * @author s230632 & s235257
 */
public record PopulationStepResult<S>(
        IterationSnapshot<S> runState,
        int evaluationsDelta,
        Map<String, Object> sharedStateVariables
) {
    public PopulationStepResult {
        sharedStateVariables = sharedStateVariables != null ? sharedStateVariables : Map.of();
    }
}