package dk.dtu.scout.population;

import dk.dtu.scout.ScoutComponent;

/**
 *
 * @param <S>
 * @author s230632 & s235257
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
