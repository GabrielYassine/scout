package dk.dtu.scout.acceptance;

import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.ScoutComponent;

import java.util.List;
import java.util.Random;

/**
 * Selects survivors from parent/child pools for the next generation.
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