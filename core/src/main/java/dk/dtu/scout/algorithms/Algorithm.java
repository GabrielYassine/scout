package dk.dtu.scout.algorithms;
import dk.dtu.scout.Component;

import java.util.Random;

public interface Algorithm<S> extends Component {
    S propose(S parent, int iteration, Random rng);
    boolean accept(double parentFitness, double childFitness, int iteration, Random rng);
}