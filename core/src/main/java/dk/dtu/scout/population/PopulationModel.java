package dk.dtu.scout.population;

import dk.dtu.scout.Component;
import dk.dtu.scout.algorithms.Algorithm;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.problems.Problem;

import java.util.Random;
import java.util.function.Supplier;

public interface PopulationModel<S> extends Component  {

    RunLog run(Supplier<Algorithm<S>> algorithmFactory, Problem<S> problem, Random rng, int maxIterations, Observer<S> observer
    );
}
