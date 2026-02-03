package dk.dtu.scout.algorithms;

import dk.dtu.scout.datatypes.RunLog;
import dk.dtu.scout.problems.Problem;

import java.util.Random;

public interface Algorithm<S> {
    RunLog<S> run(Problem<S> problem, Random rng, int maxIterations);
}
