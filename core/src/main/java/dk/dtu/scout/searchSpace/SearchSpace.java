package dk.dtu.scout.searchSpace;

import dk.dtu.scout.ScoutComponent;

import java.util.Random;

/**
 * search space contract defining the solution representation and providing a method for generating random solutions.
 * @param <S> solution representation type
 * @author s235257 & s230632
 */
public interface SearchSpace<S> extends ScoutComponent {
    S randomSolution(Random rng);
    int dimension();
}