package dk.dtu.scout.algorithms;

import dk.dtu.scout.Component;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.problems.Problem;

import java.util.Random;

public interface Algorithm<S> extends Component {
    RunLog run(Problem<S> problem, Random rng, int maxIterations, Observer<S> observer);
}
