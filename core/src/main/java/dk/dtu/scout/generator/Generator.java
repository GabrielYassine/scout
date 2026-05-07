package dk.dtu.scout.generator;

import dk.dtu.scout.ScoutComponent;

import java.util.Random;

/**
 * Generates new candidate solutions, typically via mutation or construction.
 * @author s235257 & Ahmed
 */
public interface Generator<S> extends ScoutComponent {
    S generate(Random rng);
}
