package dk.dtu.scout.population;

import dk.dtu.scout.State;
import dk.dtu.scout.acceptance.SelectionRule;
import dk.dtu.scout.generator.Generator;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.searchSpace.SearchSpace;

import java.util.Random;
import java.util.function.Supplier;

public record PopulationModelContext<S>(
        Supplier<Generator<S>> generatorFactory,
        SelectionRule<S> selection,
        SearchSpace<S> space,
        Problem<S> problem,
        Random rng,
        State sharedState
) {
}
