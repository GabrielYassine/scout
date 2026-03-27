package dk.dtu.scout.acceptance;

import dk.dtu.scout.EvaluatedSolution;
import dk.dtu.scout.ScoutComponent;

import java.util.List;
import java.util.Map;
import java.util.Random;

public interface SelectionRule<S> extends ScoutComponent {
    List<EvaluatedSolution<S>> select(
            List<EvaluatedSolution<S>> parents,
            List<EvaluatedSolution<S>> children,
            int mu,
            int iteration,
            Random rng
    );

    default void configure(Map<String, Object> params) {}
}