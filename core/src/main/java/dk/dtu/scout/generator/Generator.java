package dk.dtu.scout.generator;

import dk.dtu.scout.ScoutComponent;

import java.util.Map;
import java.util.Random;

public interface Generator<S> extends ScoutComponent {
    S generate(S solution, Random rng);
    default void configure(Map<String, Object> params) {}
}
