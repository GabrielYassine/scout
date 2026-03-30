package dk.dtu.scout;

import dk.dtu.scout.acceptance.SelectionRule;
import dk.dtu.scout.generator.Generator;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;
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
import java.util.function.Supplier;

public class SimulationRunner {

    public <S> RunLog run(
            PopulationModel<S> populationModel,
            Supplier<Generator<S>> generatorFactory,
            SelectionRule<S> selection,
            ParentSelectionRule<S> parentSelection,
            SearchSpace<S> space,
            Problem<S> problem,
            Random rng,
            List<StopCondition<S>> stopConditions,
            List<Observer<S>> observers,
            int logEveryIterations
    ) {
        if (logEveryIterations <= 0) {
            throw new IllegalArgumentException("logEveryIterations must be positive");
        }

        RunLog log = new RunLog();
        State sharedState = new State();
        sharedState.update(Map.of(
                "problem", problem,
                "dimension", space.dimension(),
                "searchSpaceId", space.id()
        ));

        List<ScoutComponent> sharedComponents = new ArrayList<>();
        sharedComponents.add(selection);
        sharedComponents.add(parentSelection);
        sharedComponents.add(space);
        sharedComponents.add(problem);
        if (stopConditions != null) {
            sharedComponents.addAll(stopConditions);
        }
        if (observers != null) {
            sharedComponents.addAll(observers);
        }

        for (ScoutComponent component : sharedComponents) {
            component.init(sharedState);
        }

        PopulationModelContext<S> context = new PopulationModelContext<>(
                generatorFactory,
                selection,
                parentSelection,
                space,
                problem,
                rng,
                sharedState
        );

        PopulationInitialization<S> initialization = populationModel.initialize(context);
        PopulationState<S> populationState = initialization.state();
        RunState<S> currentState = initialization.initialState();
        int evaluations = initialization.evaluations();

        List<ScoutComponent> stateComponents = new ArrayList<>(sharedComponents);
        if (initialization.stateComponents() != null) {
            stateComponents.addAll(initialization.stateComponents());
        }

        updateSharedState(sharedState, initialization.sharedStateVariables());
        updateComponentStateVariables(sharedState, stateComponents);

        notifyOnStart(observers, currentState, log);
        log.tick(currentState.iteration(), currentState.evaluations() - 1);
        notifyOnStep(observers, currentState, log);

        int iteration = currentState.iteration();

        while (!shouldStop(stopConditions, iteration, evaluations, currentState.bestFitness(), currentState.bestSolution())) {
            PopulationStepResult<S> stepResult = populationModel.step(context, populationState, iteration, evaluations);
            evaluations += stepResult.evaluationsDelta();
            currentState = stepResult.runState();

            updateSharedState(sharedState, stepResult.sharedStateVariables());
            updateComponentStateVariables(sharedState, stateComponents);

            if ((currentState.iteration() + 1) % logEveryIterations == 0) {
                log.tick(currentState.iteration(), currentState.evaluations() - 1);
                notifyOnStep(observers, currentState, log);
            }

            iteration++;
        }

        if (((currentState.iteration() + 1) % logEveryIterations) != 0) {
            log.tick(currentState.iteration(), currentState.evaluations() - 1);
            notifyOnStep(observers, currentState, log);
        }

        notifyOnEnd(observers, currentState, log);
        return log;
    }

    private void updateSharedState(State sharedState, Map<String, Object> vars) {
        if (sharedState == null || vars == null || vars.isEmpty()) {
            return;
        }
        sharedState.update(vars);
    }

    private void updateComponentStateVariables(State state, List<ScoutComponent> components) {
        if (state == null || components == null || components.isEmpty()) {
            return;
        }
        Map<String, Object> combinedStateVariables = new HashMap<>();
        for (ScoutComponent component : components) {
            combinedStateVariables.putAll(component.getStateVariables(state));
        }
        state.update(combinedStateVariables);
    }

    private <S> boolean shouldStop(
            List<StopCondition<S>> conditions,
            int iteration,
            int evaluations,
            double bestFitness,
            S bestSolution
    ) {
        if (conditions == null || conditions.isEmpty()) {
            return false;
        }
        for (StopCondition<S> condition : conditions) {
            if (condition.shouldStop(iteration, evaluations, bestFitness, bestSolution)) {
                return true;
            }
        }
        return false;
    }

    private <S> void notifyOnStart(List<Observer<S>> observers, RunState<S> state, RunLog log) {
        if (observers == null) {
            return;
        }
        for (Observer<S> observer : observers) {
            observer.onStart(state, log);
        }
    }

    private <S> void notifyOnStep(List<Observer<S>> observers, RunState<S> state, RunLog log) {
        if (observers == null) {
            return;
        }
        for (Observer<S> observer : observers) {
            observer.onStep(state, log);
        }
    }

    private <S> void notifyOnEnd(List<Observer<S>> observers, RunState<S> state, RunLog log) {
        if (observers == null) {
            return;
        }
        for (Observer<S> observer : observers) {
            observer.onEnd(state, log);
        }
    }
}
