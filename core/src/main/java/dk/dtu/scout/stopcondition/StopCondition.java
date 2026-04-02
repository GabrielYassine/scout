package dk.dtu.scout.stopcondition;

import dk.dtu.scout.ScoutComponent;

/**
 * Termination criterion for a run.
 */
public interface StopCondition<S> extends ScoutComponent {
    boolean shouldStop(int iteration, int evaluations, double bestFitness, S bestSolution);
}