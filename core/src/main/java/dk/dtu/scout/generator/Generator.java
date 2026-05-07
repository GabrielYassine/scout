package dk.dtu.scout.generator;

import dk.dtu.scout.ScoutComponent;

import java.util.Random;

/**
 * Generates new candidate solutions, typically via mutation or construction.
 * @param <S> the solution representation type
 * @author s235257 & s230632
 */
public interface Generator<S> extends ScoutComponent {
    S generate(Random rng);
}
