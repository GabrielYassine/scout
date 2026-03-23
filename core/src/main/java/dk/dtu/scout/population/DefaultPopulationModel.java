package dk.dtu.scout.population;
import dk.dtu.scout.Parameter;
import dk.dtu.scout.ScoutComponent;
import dk.dtu.scout.State;
import dk.dtu.scout.acceptance.AcceptanceRule;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;
import dk.dtu.scout.generator.Generator;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.searchSpace.SearchSpace;
import dk.dtu.scout.stopcondition.StopCondition;
import dk.dtu.scout.stopcondition.StopConditions;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

@Component
@Scope("prototype")
public class DefaultPopulationModel<S> implements PopulationModel<S> {

    private int lambda = 1; // (1+1) by default

    @Override
    public String id() {
        return "default";
    }

    @Override
    public String displayName() {
        return "Default Population Model";
    }

    @Override
    public String description() {
        return "Single run with a single algorithm instance";
    }

    @Override
    public List<Parameter> params() {
        return List.of(new Parameter("lambda", "lambda (Children Amount)", "int", lambda, 1.0, null));
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) return;
        if (params.containsKey("lambda")) {
            int value = ((Number) params.get("lambda")).intValue();
            if (value <= 0) {
                throw new IllegalArgumentException("Lambda must be positive");
            }
            this.lambda = value;
        }
    }

    private List<ScoutComponent> initializeComponents(
            Generator<S> generator,
            AcceptanceRule acceptance,
            SearchSpace<S> space,
            Problem<S> problem,
            List<StopCondition<S>> stopConditions,
            Observer<S> observer
    ) {
        List<ScoutComponent> components = new ArrayList<>();
        components.add(generator);
        components.add(acceptance);
        components.add(space);
        components.add(problem);
        components.addAll(stopConditions);
        components.add(observer);
        return components;
    }

    @Override
    public RunLog run(
            Supplier<Generator<S>> generatorFactory,
            AcceptanceRule acceptance,
            SearchSpace<S> space,
            Problem<S> problem,
            Random rng,
            List<StopCondition<S>> stopConditions,
            Observer<S> observer,
            int logEveryIterations
    ) {
        RunLog log = new RunLog();
        int logInterval = logEveryIterations <= 0 ? 1 : logEveryIterations;
        State varState = new State();

        Generator<S> generator = generatorFactory.get();

        // Initialize components list
        List<ScoutComponent> components = initializeComponents(generator, acceptance, space, problem, stopConditions, observer);

        // Store problem in state so generators can access it
        varState.update(Map.of("problem", problem));

        // Initialize all components with state
        for (ScoutComponent component : components) {
            component.init(varState);
        }

        // 1) Initialize parent
        S current = space.randomSolution(rng);
        double currentFitness = problem.fitness(current);
        S best = current;
        double bestFitness = currentFitness;

        int evaluations = 1;
        int iteration = 0;

        // Initial state
        RunState<S> initial = new RunState<>(iteration, evaluations, current, currentFitness, best, bestFitness, false);
        observer.onStart(initial, log);
        log.tick(iteration, evaluations);
        observer.onStep(initial, log);

        List<S> generationSolutions = new ArrayList<>();
        List<Double> generationFitness = new ArrayList<>();

        // 2) Loop until stop condition is met
        while (!StopConditions.shouldStop(stopConditions, iteration, evaluations, bestFitness, best)) {
            // Update state variables from all components first
            Map<String, Object> combinedStateVariables = new HashMap<>();
            // Order of state variables in the map (for consistency and readability):
            // 1) population model
            // 1) generator
            // 2) acceptance
            // 3) space
            // 4) problem
            // 5) stop
            // 6) observer
            varState.update(Map.of(
                    "current", current,
                    "best", best,
                    "bestFitness", bestFitness,
                    "currentFitness", currentFitness,
                    "generationSolutions", generationSolutions,
                    "generationFitness", generationFitness
            ));

            for (ScoutComponent component : components) {
                combinedStateVariables.putAll(component.getStateVariables(varState));
            }
            varState.update(combinedStateVariables);

            S bestChild = null;
            double bestChildFitness = Double.NEGATIVE_INFINITY;
            generationSolutions.clear();
            generationFitness.clear();

            // 3) Generate λ children and evaluate them, keep the best
            for (int k = 0; k < lambda; k++) {
                S child = generator.generate(rng);
                double childFitness = problem.fitness(child);

                evaluations++;
                generationSolutions.add(child);
                generationFitness.add(childFitness);

                if (childFitness > bestChildFitness) {
                    bestChildFitness = childFitness;
                    bestChild = child;
                }
            }

            // 4) decide whether to accept the best child as the new current solution
            boolean accepted = acceptance.accept(currentFitness, bestChildFitness, iteration, rng);

            // If accepted, update current solution and fitness
            if (accepted) {
                current = bestChild;
                currentFitness = bestChildFitness;

                if (currentFitness > bestFitness) {
                    best = current;
                    bestFitness = currentFitness;
                }
            }

            RunState<S> stateLog = new RunState<>(iteration, evaluations, current, currentFitness, best, bestFitness, accepted);
            if (stateLog.iteration() % logInterval == 0) {
                log.tick(stateLog.iteration(), stateLog.evaluations());
                observer.onStep(stateLog, log);
            }
            iteration++;
        }

        RunState<S> finalState = new RunState<>(iteration - 1, evaluations, current, currentFitness, best, bestFitness, false);
        if ((finalState.iteration() % logInterval) != 0) {
            log.tick(finalState.iteration(), finalState.evaluations());
            observer.onStep(finalState, log);
        }
        observer.onEnd(finalState, log);

        return log;
    }
}