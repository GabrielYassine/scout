
package dk.dtu.scout.stopcondition;

import java.util.List;

public class StopConditions{
    private StopConditions() {}

    public static <S>  boolean shouldStop(List<StopCondition<S>> conditions, int iteration, int evaluations, double bestFitness, S bestSolution) {
        for (StopCondition<S> c : conditions) {
            if (c.shouldStop(iteration, evaluations, bestFitness, bestSolution)) {
                return true;
            }
        }
        return false;
    }
}
