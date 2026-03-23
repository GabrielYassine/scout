package dk.dtu.scout.population;

import dk.dtu.scout.ScoutComponent;
import dk.dtu.scout.acceptance.AcceptanceRule;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.generator.Generator;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.searchSpace.SearchSpace;
import dk.dtu.scout.stopcondition.StopCondition;

import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

public interface PopulationModel<S> extends ScoutComponent {

    RunLog run(
            Supplier<Generator<S>> generatorFactory,
            AcceptanceRule acceptance,
            SearchSpace<S> space,
            Problem<S> problem,
            Random rng,
            StopCondition<S> stop,
            Observer<S> observer,
            int logEveryIterations
    );
    default void configure(Map<String, Object> params) {}
}
