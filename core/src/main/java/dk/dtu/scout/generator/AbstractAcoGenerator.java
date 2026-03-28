package dk.dtu.scout.generator;

import dk.dtu.scout.Parameter;

import java.util.List;
import java.util.Map;

public abstract class AbstractAcoGenerator<S> implements Generator<S> {

    protected double evaporationRate = 0.1;
    protected double alpha = 1.0;
    protected double beta = 2.0;

    protected List<Parameter> acoParams() {
        return List.of(
                new Parameter("evaporationRate", "Pheromone Evaporation Rate", "double", evaporationRate, 0.0, 1.0),
                new Parameter("alpha", "Pheromone Influence", "double", alpha, 0.1, 5.0),
                new Parameter("beta", "Heuristic/Complement Influence", "double", beta, 0.1, 10.0)
        );
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) return;

        if (params.containsKey("evaporationRate")) {
            double value = ((Number) params.get("evaporationRate")).doubleValue();
            if (value < 0.0 || value > 1.0) {
                throw new IllegalArgumentException("Evaporation rate must be between 0 and 1");
            }
            this.evaporationRate = value;
        }
        if (params.containsKey("alpha")) {
            double value = ((Number) params.get("alpha")).doubleValue();
            if (value < 0.1 || value > 5.0) {
                throw new IllegalArgumentException("Alpha must be between 0.1 and 5.0");
            }
            this.alpha = value;
        }
        if (params.containsKey("beta")) {
            double value = ((Number) params.get("beta")).doubleValue();
            if (value < 0.1 || value > 10.0) {
                throw new IllegalArgumentException("Beta must be between 0.1 and 10.0");
            }
            this.beta = value;
        }
    }
}
