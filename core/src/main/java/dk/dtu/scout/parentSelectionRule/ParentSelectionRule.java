package dk.dtu.scout.parentSelectionRule;

import dk.dtu.scout.EvaluatedSolution;
import dk.dtu.scout.ScoutComponent;

import java.util.List;
import java.util.Map;
import java.util.Random;

public interface ParentSelectionRule<S> extends ScoutComponent {
    EvaluatedSolution<S> select(List<EvaluatedSolution<S>> parents, Random rng);
    default void configure(Map<String, Object> params) {}
}
