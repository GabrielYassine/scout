package dk.dtu.scout.population;

import dk.dtu.scout.logging.IterationSnapshot;

import java.util.Map;

/**
 * Result of one {@link PopulationModel#step(PopulationModelContext, PopulationState, int, int)} call.
 *
 * @param runState current public snapshot used by observers, stop conditions and logging
 * @param evaluationsDelta number of fitness evaluations performed in this step
 * @param sharedStateVariables optional key/value updates for the shared per-run state blackboard
 * @param <S> solution type
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