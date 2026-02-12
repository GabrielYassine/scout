package dk.dtu.scout.population;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.algorithms.Algorithm;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.problems.Problem;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

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
    public RunLog run(Supplier<Algorithm<S>> algorithmFactory, Problem<S> problem, Random rng, int maxIterations, Observer<S> observer) {
        return algorithmFactory.get().run(problem, rng, maxIterations, observer);
    }
}
