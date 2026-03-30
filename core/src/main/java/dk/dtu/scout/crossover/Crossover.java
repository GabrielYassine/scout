package dk.dtu.scout.crossover;

import dk.dtu.scout.ScoutComponent;

import java.util.List;
import java.util.Map;
import java.util.Random;


public interface Crossover<S> extends ScoutComponent {
    S crossover(Random rng);
    default void configure(Map<String, Object> params) {}
}