package dk.dtu.scout.population;

import dk.dtu.scout.ScoutComponent;

/**
 * Interface for population-based models in the Scout framework.
 * A population model defines how a population of candidate solutions is initialized and evolved over time.
 * @param <S> the type representing an individual solution in the population
 */
public interface PopulationModel<S> extends ScoutComponent {
    PopulationInitialization<S> initialize(PopulationModelContext<S> context);

    PopulationStepResult<S> step(
            PopulationModelContext<S> context,
            PopulationState<S> state,
            int iteration,
            int evaluations
    );
}
