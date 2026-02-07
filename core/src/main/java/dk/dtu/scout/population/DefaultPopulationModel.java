package dk.dtu.scout.population;

import dk.dtu.scout.algorithms.Algorithm;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.problems.Problem;

import java.util.Random;
import java.util.function.Supplier;

public class DefaultPopulationModel<S> implements PopulationModel<S> {

    @Override
    public RunLog run(Supplier<Algorithm<S>> algorithmFactory, Problem<S> problem, Random rng, int maxIterations, Observer<S> observer) {
        return algorithmFactory.get().run(problem, rng, maxIterations, observer);
    }
}
