package dk.dtu.scout.stopcondition;

import dk.dtu.scout.Component;

import java.util.Map;

public interface StopCondition<S> extends Component {
    boolean shouldStop(int iteration, int evaluations, double bestFitness, S bestSolution);
    default void configure(Map<String, Object> params) {}
}