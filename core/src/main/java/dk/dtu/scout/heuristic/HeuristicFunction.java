package dk.dtu.scout.heuristic;

import dk.dtu.scout.ScoutComponent;

import java.util.Map;

public interface HeuristicFunction<S> extends ScoutComponent {
    void initialize(Object problemData);
    double getHeuristic(int from, int to);
    default void configure(Map<String, Object> params) {}
}
