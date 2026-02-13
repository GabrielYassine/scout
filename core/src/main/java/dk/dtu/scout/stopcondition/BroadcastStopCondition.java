
package dk.dtu.scout.stopcondition;

import dk.dtu.scout.Parameter;

import java.util.List;
import java.util.Map;

public class BroadcastStopCondition<S> implements StopCondition<S> {

    private final List<StopCondition<S>> conditions;

    public BroadcastStopCondition(List<StopCondition<S>> conditions) {
        this.conditions = conditions;
    }

    @Override
    public boolean shouldStop(int iteration, int evaluations, double bestFitness, S bestSolution) {
        for (StopCondition<S> c : conditions) {
            if (c.shouldStop(iteration, evaluations, bestFitness, bestSolution)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public String id() {
        return "";
    }

    @Override
    public String displayName() {
        return "";
    }

    @Override
    public String description() {
        return "";
    }

    @Override
    public List<Parameter> params() {
        return List.of(
        );
    }

}
