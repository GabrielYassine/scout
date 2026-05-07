package dk.dtu.scout.searchSpace;

import dk.dtu.scout.ScoutComponent;

import java.util.Random;

/**
 *
 * @author s235257 & s230632
 */
public interface SearchSpace<S> extends ScoutComponent {
    S randomSolution(Random rng);
    int dimension();
}