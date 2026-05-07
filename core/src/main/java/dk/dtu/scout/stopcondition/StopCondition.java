package dk.dtu.scout.stopcondition;

import dk.dtu.scout.ScoutComponent;

/**
 *
 * @author s235257 & s230632
 */
public interface StopCondition<S> extends ScoutComponent {
    boolean shouldStop(int iteration, int evaluations, double bestFitness, S bestSolution);
}