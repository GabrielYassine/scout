package dk.dtu.scout.parentSelectionRule;

import dk.dtu.scout.EvaluatedSolution;
import dk.dtu.scout.ScoutComponent;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Selects parents from the current population for variation operators.
 */
public interface ParentSelectionRule<S> extends ScoutComponent {
    List<EvaluatedSolution<S>> selectParents(List<EvaluatedSolution<S>> parents, Random rng);
}