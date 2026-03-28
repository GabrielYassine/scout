package dk.dtu.scout.population;

import dk.dtu.scout.ScoutComponent;

import java.util.Map;

public interface PopulationModel<S> extends ScoutComponent {
    PopulationInitialization<S> initialize(PopulationModelContext<S> context);

    PopulationStepResult<S> step(
            PopulationModelContext<S> context,
            PopulationState<S> state,
            int iteration,
            int evaluations
    );

    default void configure(Map<String, Object> params) {}
}
