package dk.dtu.scout.heuristic;

import dk.dtu.scout.Component;

import java.util.Map;

public interface HeuristicFunction<S> extends Component {
    void initialize(Object problemData);
    double getHeuristic(int from, int to);
    default void configure(Map<String, Object> params) {}
}
