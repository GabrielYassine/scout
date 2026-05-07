package dk.dtu.scout.parentSelectionRule;

import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.ScoutComponent;

import java.util.List;
import java.util.Random;

/**
 *
 * @param <S>
 * @author s230632
 */
public interface ParentSelectionRule<S> extends ScoutComponent {
    List<EvaluatedSolution<S>> selectParents(List<EvaluatedSolution<S>> parents, Random rng);
}