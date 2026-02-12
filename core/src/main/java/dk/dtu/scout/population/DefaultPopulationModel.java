package dk.dtu.scout.population;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.algorithms.Algorithm;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.searchSpace.SearchSpace;
import dk.dtu.scout.stopcondition.StopCondition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

@Component
public class DefaultPopulationModel<S> implements PopulationModel<S> {

    @Override
    public String id() {
        return "default";
    }

    @Override
    public String displayName() {
        return "Default Population Model";
    }

    @Override
    public String description() {
        return "Single run with a single algorithm instance";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public RunLog run(Supplier<Algorithm<S>> algorithmFactory, SearchSpace<S> space, Problem<S> problem, Random rng, StopCondition<S> stop, Observer<S> observer) {
        return algorithmFactory.get().run(space,problem, rng, stop, observer);
    }
}
