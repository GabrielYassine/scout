package dk.dtu.scout.stopcondition;

import dk.dtu.scout.ScoutComponent;

import java.util.Map;

/**
 * Termination criterion for a run.
 */
public interface StopCondition<S> extends ScoutComponent {
    boolean shouldStop(int iteration, int evaluations, double bestFitness, S bestSolution);
    default void configure(Map<String, Object> params) {}
}