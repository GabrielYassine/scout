package dk.dtu.scout.construction;

import dk.dtu.scout.ScoutComponent;
import dk.dtu.scout.heuristic.HeuristicFunction;
import dk.dtu.scout.pheromone.PheromoneModel;

import java.util.Map;
import java.util.Random;
public interface ConstructionPolicy<S> extends ScoutComponent {
    S constructSolution(PheromoneModel<S> pheromones, HeuristicFunction<S> heuristics, Random rng);
    default void configure(Map<String, Object> params) {}
}
