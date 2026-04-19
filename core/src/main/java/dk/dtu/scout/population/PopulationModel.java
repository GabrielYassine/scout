package dk.dtu.scout.population;

import dk.dtu.scout.ScoutComponent;

/**
 * Population model contract for one evolutionary algorithm variant.
 *
 * <p>A {@code PopulationModel} owns the algorithm's internal, strongly-typed state
 * through {@link PopulationState}. This internal state is opaque to the runner and
 * to observers.
 *
 * <p>On each step, the model must return:
 * <ul>
 *   <li>a {@link IterationSnapshot} snapshot for logging/observers/stop conditions,</li>
 *   <li>the number of evaluations performed in that step,</li>
 *   <li>optional shared state variables for cross-component communication.</li>
 * </ul>
 *
 * <p>Mutation/crossover/parent-selection orchestration is model-specific: the model
 * decides if and how {@code Generator}, {@code Crossover}, and {@code ParentSelectionRule}
 * are applied.
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
