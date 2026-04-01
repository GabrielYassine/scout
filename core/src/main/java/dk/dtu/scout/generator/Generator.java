package dk.dtu.scout.generator;

import dk.dtu.scout.ScoutComponent;

import java.util.Map;
import java.util.Random;

/**
 * Generates new candidate solutions, typically via mutation or construction.
 */
public interface Generator<S> extends ScoutComponent {
    S generate(Random rng);
    default void configure(Map<String, Object> params) {}
}
