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
 * Shared context passed to population models during initialization and execution.
 * The context contains all components needed by a population model to generate,
 * evaluate, and select solutions.
 * @param generatorFactory factory for creating generator instances
 * @param parentSelection rule used to select parents before offspring generation
 * @param crossover optional crossover operator used to combine selected parents
 * @param selection rule used to select the next parent population
 * @param space search space used to create and validate solutions
 * @param problem optimization problem used to evaluate fitness
 * @param rng random number generator used during the run
 * @param sharedState shared state used for communication between components
 * @param <S> solution representation type
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
