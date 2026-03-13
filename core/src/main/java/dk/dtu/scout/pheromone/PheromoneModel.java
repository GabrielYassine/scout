package dk.dtu.scout.pheromone;

import dk.dtu.scout.Component;

import java.util.Map;

public interface PheromoneModel<S> extends Component {
    void initialize(int dimension);
    double getPheromone(int from, int to);
    void setPheromone(int from, int to, double value);
    void evaporate(double evaporationRate);
    void deposit(S solution, double amount);
    int getDimension();
    default void configure(Map<String, Object> params) {}
}
