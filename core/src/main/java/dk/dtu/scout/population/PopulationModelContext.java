package dk.dtu.scout.population;

import dk.dtu.scout.State;
import dk.dtu.scout.acceptance.SelectionRule;
import dk.dtu.scout.crossover.Crossover;
import dk.dtu.scout.generator.Generator;
import dk.dtu.scout.parentSelectionRule.ParentSelectionRule;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.searchSpace.SearchSpace;

import java.util.Random;
import java.util.function.Supplier;

/**
 * Immutable dependency bundle passed to {@link PopulationModel}.
 *
 * <p>Contains all collaborators needed by a model step. The model controls how
 * and when these collaborators are used.
 *
 * <p>The {@code sharedState} field is a cross-component blackboard for values that
 * may be consumed outside the model (observers, stop conditions, other components).
 * Model-private data should remain in {@link PopulationState} implementations.
 */
public record PopulationModelContext<S>(
        Supplier<Generator<S>> generatorFactory,
        ParentSelectionRule<S> parentSelection,
        Crossover<S> crossover,
        SelectionRule<S> selection,
        SearchSpace<S> space,
        Problem<S> problem,
        Random rng,
        State sharedState
) {
}
