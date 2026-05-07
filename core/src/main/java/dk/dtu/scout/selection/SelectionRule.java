package dk.dtu.scout.selection;

import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.ScoutComponent;

import java.util.List;
import java.util.Random;

/**
 * Contract for selection rules, which define how to select the next parent population from the current parents and offspring.
 * @author s235257 & s230632
 */
public interface SelectionRule<S> extends ScoutComponent {
    List<EvaluatedSolution<S>> select(
            List<EvaluatedSolution<S>> parents,
            List<EvaluatedSolution<S>> children,
            int mu,
            int iteration,
            Random rng
    );
}