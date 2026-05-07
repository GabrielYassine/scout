package dk.dtu.scout;

import dk.dtu.scout.selection.SelectionRule;
import dk.dtu.scout.crossover.Crossover;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.generator.Generator;
import dk.dtu.scout.logging.IterationSnapshot;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.parentSelectionRule.ParentSelectionRule;
import dk.dtu.scout.population.PopulationInitialization;
import dk.dtu.scout.population.PopulationModel;
import dk.dtu.scout.population.PopulationModelContext;
import dk.dtu.scout.population.PopulationState;
import dk.dtu.scout.population.PopulationStepResult;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.searchSpace.SearchSpace;
import dk.dtu.scout.stopcondition.StopCondition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;

/**
 * Coordinates the execution of a complete optimization run.
 * @author s235257 & s230632
 */
public class SimulationRunner {

    /**
     * Execute a full run with the provided components and return the aggregated log.
     * @param populationModel population model that controls the algorithmic loop
     * @param generatorFactory factory for creating generator instances
     * @param crossover optional crossover operator, or null if crossover is not used
     * @param parentSelection optional parent selection rule
     * @param selection selection rule used to choose the next population
     * @param space search space used by the run
     * @param problem optimization problem used to evaluate solutions
     * @param rng random number generator used during the run
     * @param stopConditions stop conditions that can terminate the run
     * @param observers observers that record run data
     * @param logEveryEvaluations logging interval measured in fitness evaluations
     * @return run log containing evaluations and observer series
     * @param <S> solution representation type
     */
    public <S> RunLog run(
        PopulationModel<S> populationModel,
        Supplier<Generator<S>> generatorFactory,
        Crossover<S> crossover,
        ParentSelectionRule<S> parentSelection,
        SelectionRule<S> selection,
        SearchSpace<S> space,
        Problem<S> problem,
        Random rng,
        List<StopCondition<S>> stopConditions,
        List<Observer<S>> observers,
        int logEveryEvaluations
    ) {
        // #1 Create the per-run output log and shared runtime state.
        RunLog log = new RunLog();
        State sharedState = new State();

        // #2 Seed the shared state with core invariants that other components may need.
        sharedState.update(Map.of(
            StateKeys.PROBLEM, problem,
            StateKeys.DIMENSION, space.dimension(),
            StateKeys.SEARCH_SPACE_ID, space.id()
        ));

        // #3 Collect all components that participate in shared initialization/state publication.
        List<ScoutComponent> sharedComponents = new ArrayList<>();

        // Must have components:
        sharedComponents.add(selection);
        sharedComponents.add(space);
        sharedComponents.add(problem);

        if (crossover != null) {
            sharedComponents.add(crossover);
        }
        if (parentSelection != null) {
            sharedComponents.add(parentSelection);
        }

        sharedComponents.addAll(stopConditions);
        sharedComponents.addAll(observers);

        // #4 Initialize all shared components with the shared run state.
        for (ScoutComponent component : sharedComponents) {
            component.init(sharedState);
        }

        // #5 Build the immutable dependency bundle passed into the population model.
        PopulationModelContext<S> context = new PopulationModelContext<>(
            generatorFactory,
            parentSelection,
            crossover,
            selection,
            space,
            problem,
            rng,
            sharedState
        );

        // #6 Ask the population model to initialize its internal state and public initial snapshot.
        PopulationInitialization<S> initialization = populationModel.initialize(context);
        PopulationState<S> populationState = initialization.state();
        IterationSnapshot<S> currentState = initialization.initialState();
        int evaluations = initialization.evaluations();

        // #7 Build the complete list of components that may publish runtime state variables.
        List<ScoutComponent> stateComponents = new ArrayList<>(sharedComponents);
        stateComponents.addAll(initialization.stateComponents());

        // #8 Merge any shared-state values produced during initialization.
        sharedState.update(initialization.sharedStateVariables());
        updateComponentStateVariables(sharedState, stateComponents);

        // #9 Notify observers that the run has started.
        notifyOnStart(observers, currentState, log);

        // #11 Record the initial tick and notify observers of the initial visible state.
        log.tick(currentState.evaluations() - 1);
        notifyOnStep(observers, currentState, log);

        int lastLoggedEvaluation = currentState.evaluations();

        int iteration = currentState.iteration();

        // #12 Main execution loop:
        // keep iterating until one of the configured stop conditions becomes true.
        while (!shouldStop(stopConditions, iteration, evaluations, currentState.bestFitness(), currentState.bestSolution())) {

            // #13 Check for thread interruption and abort.
            if (Thread.currentThread().isInterrupted()) {
                throw new CancellationException("Run cancelled");
            }

            // #14 Delegate one algorithmic step to the selected population model.
            PopulationStepResult<S> stepResult = populationModel.step(context, populationState, iteration, evaluations);

            // #15 Update the global evaluation count and current public iteration snapshot.
            evaluations += stepResult.evaluationsDelta();
            currentState = stepResult.runState();

            // #16 Merge runtime state published by the step and by participating components.
            sharedState.update(stepResult.sharedStateVariables());
            updateComponentStateVariables(sharedState, stateComponents);

            // #17 Only log/notify observers at the configured cadence.
            if (currentState.evaluations() - lastLoggedEvaluation >= logEveryEvaluations) {
                // #18 Record a visible tick and let observers write their data series into the log.
                log.tick(currentState.evaluations() - 1);
                notifyOnStep(observers, currentState, log);
                lastLoggedEvaluation = currentState.evaluations();
            }

            // #19 Advance the runner’s loop counter.
            iteration++;
        }

        // #20 If the run stopped between logging points, ensure the terminal state is still logged.
        if (currentState.evaluations() != lastLoggedEvaluation) {
            log.tick(currentState.evaluations() - 1);
            notifyOnStep(observers, currentState, log);
        }

        // #22 Notify observers that the run has ended and return the final accumulated log.
        notifyOnEnd(observers, currentState, log);
        return log;
    }
    /**
     * Helper method to collect and merge state variables from all components that publish runtime state.
     * @param state shared state to update with collected variables
     * @param components components that may publish state variables
     */
    private void updateComponentStateVariables(State state, List<ScoutComponent> components) {
        Map<String, Object> combinedStateVariables = new HashMap<>();
        for (ScoutComponent component : components) {
            combinedStateVariables.putAll(component.getStateVariables(state));
        }
        state.update(combinedStateVariables);
    }
   /**
     * Helper method to evaluate all stop conditions and determine if the run should terminate.
     * @param conditions list of stop conditions to evaluate
     * @param iteration current iteration number
     * @param evaluations total number of fitness evaluations performed
     * @param bestFitness best fitness value found so far
     * @param bestSolution best solution found so far
     * @return true if any stop condition is met, false otherwise
     */
    private <S> boolean shouldStop(
        List<StopCondition<S>> conditions,
        int iteration,
        int evaluations,
        double bestFitness,
        S bestSolution
    ) {
        for (StopCondition<S> condition : conditions) {
            if (condition.shouldStop(iteration, evaluations, bestFitness, bestSolution)) {
                return true;
            }
        }
        return false;
    }
    
    private <S> void notifyOnStart(List<Observer<S>> observers, IterationSnapshot<S> state, RunLog log) {
        for (Observer<S> observer : observers) {
            observer.onStart(state, log);
        }
    }

    private <S> void notifyOnStep(List<Observer<S>> observers, IterationSnapshot<S> state, RunLog log) {
        for (Observer<S> observer : observers) {
            observer.onStep(state, log);
        }
    }

    private <S> void notifyOnEnd(List<Observer<S>> observers, IterationSnapshot<S> state, RunLog log) {
        for (Observer<S> observer : observers) {
            observer.onEnd(state, log);
        }
    }
}