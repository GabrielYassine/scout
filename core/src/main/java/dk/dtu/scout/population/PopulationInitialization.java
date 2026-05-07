package dk.dtu.scout.population;

import dk.dtu.scout.ScoutComponent;
import dk.dtu.scout.logging.IterationSnapshot;

import java.util.List;
import java.util.Map;


/**
 * Result returned when a population model is initialized.
 * It contains the internal population state, the initial public run snapshot,
 * the number of fitness evaluations used during initialization, and any state
 * variables or components that should be registered with the simulation runner.
 * @param state internal state used by the population model during later steps
 * @param initialState initial snapshot shown to observers and logs
 * @param evaluations number of fitness evaluations used during initialization
 * @param sharedStateVariables state variables published during initialization
 * @param stateComponents additional components that can publish state variables during the run
 * @param <S> solution representation type
 * @author s230632 & s235257
 */
public record PopulationInitialization<S>(
    PopulationState<S> state,
    IterationSnapshot<S> initialState,
    int evaluations,
    Map<String, Object> sharedStateVariables,
    List<ScoutComponent> stateComponents
) {
    public PopulationInitialization {
        sharedStateVariables = sharedStateVariables != null ? sharedStateVariables : Map.of();
        stateComponents = stateComponents != null ? stateComponents : List.of();
    }
}