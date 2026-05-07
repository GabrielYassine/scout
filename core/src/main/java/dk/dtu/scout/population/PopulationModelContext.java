package dk.dtu.scout.population;

import dk.dtu.scout.State;
import dk.dtu.scout.selection.SelectionRule;
import dk.dtu.scout.crossover.Crossover;
import dk.dtu.scout.generator.Generator;
import dk.dtu.scout.parentSelectionRule.ParentSelectionRule;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.searchSpace.SearchSpace;

import java.util.Random;
import java.util.function.Supplier;

/**
 *
 * @param <S>
 * @author s230632 & s235257
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
