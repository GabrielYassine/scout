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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
            StopCondition<S> stop,
            Observer<S> observer
    ) {
        List<ScoutComponent> components = new ArrayList<>();
        components.add(generator);
        components.add(acceptance);
        components.add(space);
        components.add(problem);
        components.add(stop);
        components.add(observer);
        return components;
    }

    @Override
    public RunLog run(
            Generator<S> generator,
            AcceptanceRule acceptance,
            SearchSpace<S> space,
            Problem<S> problem,
            Random rng,
            StopCondition<S> stop,
            Observer<S> observer
    ) {
        RunLog log = new RunLog();
        State varState = new State();

        // Initialize components list
        List<ScoutComponent> components = initializeComponents(generator, acceptance, space, problem, stop, observer);

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

        // 2) Loop until stop condition is met
        while (!stop.shouldStop(iteration, evaluations, bestFitness, best)) {
            S bestChild = null;
            double bestChildFitness = Double.NEGATIVE_INFINITY;

            // 3) Generate λ children and evaluate them, keep the best
            for (int k = 0; k < lambda; k++) {
                S child = generator.generate(current, rng);
                double childFitness = problem.fitness(child);
                evaluations++;

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

            Map<String, Object> combinedStateVariables = new HashMap<>();
            for (ScoutComponent component : components) {
                combinedStateVariables.putAll(component.getStateVariables());
            }

            varState.update(Map.of(
                    "best", best,
                    "bestFitness", bestFitness
            ));
            varState.update(combinedStateVariables);

            // log global state
            RunState<S> stateLog = new RunState<>(iteration, evaluations, current, currentFitness, best, bestFitness, accepted);
            log.tick(stateLog.iteration(), stateLog.evaluations());
            observer.onStep(stateLog, log);
            iteration++;
        }

        RunState<S> finalState = new RunState<>(iteration - 1, evaluations, current, currentFitness, best, bestFitness, false);
        observer.onEnd(finalState, log);

        return log;
    }
}