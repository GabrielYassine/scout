package dk.dtu.scout.generator;

import dk.dtu.scout.Parameter;

import java.util.List;
import java.util.Map;

public abstract class AbstractAcoGenerator<S> implements Generator<S> {

    protected double evaporationRate = 0.1;

    protected List<Parameter> evaporationParams() {
        return List.of(
                new Parameter("evaporationRate", "Pheromone Evaporation Rate", "double", evaporationRate, 0.0, 1.0)
        );
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) {
            return;
        }

        if (params.containsKey("evaporationRate")) {
            double value = ((Number) params.get("evaporationRate")).doubleValue();
            if (value < 0.0 || value > 1.0) {
                throw new IllegalArgumentException("Evaporation rate must be between 0 and 1");
            }
            this.evaporationRate = value;
        }
    }
}
