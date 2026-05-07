package dk.dtu.scout.population;

import dk.dtu.scout.logging.IterationSnapshot;

import java.util.Map;

/**
 * Result returned after one population model step.
 * @param runState public snapshot after the step has been performed
 * @param evaluationsDelta number of fitness evaluations used during this step
 * @param sharedStateVariables state variables published by the population model
 * @param <S> solution representation type
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