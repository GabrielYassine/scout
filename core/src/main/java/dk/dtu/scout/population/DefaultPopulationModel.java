package dk.dtu.scout.population;
import dk.dtu.scout.Parameter;
import dk.dtu.scout.ScoutComponent;
import dk.dtu.scout.State;
import dk.dtu.scout.acceptance.AcceptanceRule;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;
import dk.dtu.scout.generator.Generator;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.observer.Observers;
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

    private int mu = 1; // parents, default 1
    private int lambda = 1; // children, default 1

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
        return List.of(
                new Parameter("mu", "mu (Parents Amount)", "int", mu, 1.0, null),
                new Parameter("lambda", "lambda (Children Amount)", "int", lambda, 1.0, null)
        );
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) return;
        if (params.containsKey("mu")) {
            int value = ((Number) params.get("mu")).intValue();
            if (value <= 0) {
                throw new IllegalArgumentException("Mu must be positive");
            }
            this.mu = value;
        }
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
            List<Observer<S>> observers
    ) {
        List<ScoutComponent> components = new ArrayList<>();
        components.add(generator);
        components.add(acceptance);
        components.add(space);
        components.add(problem);
        components.addAll(stopConditions);
        components.addAll(observers);
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
            List<Observer<S>> observers,
            int logEveryIterations
    ) {
        RunLog log = new RunLog();
        int logInterval = logEveryIterations <= 0 ? 1 : logEveryIterations;
        State varState = new State();

        Generator<S> generator = generatorFactory.get();

        // Initialize components list
        List<ScoutComponent> components = initializeComponents(generator, acceptance, space, problem, stopConditions, observers);

        // Store problem in state so generators can access it
        varState.update(Map.of("problem", problem));

        // Initialize all components with state
        for (ScoutComponent component : components) {
            component.init(varState);
        }

        // 1) Initialize parents
        List<S> parents = new ArrayList<>(mu);
        List<Double> parentsFitness = new ArrayList<>(mu);
        S current = null;
        double currentFitness = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < mu; i++) {
            S parent = space.randomSolution(rng);
            double parentFitness = problem.fitness(parent);
            parents.add(parent);
            parentsFitness.add(parentFitness);
            if (parentFitness > currentFitness) {
                currentFitness = parentFitness;
                current = parent;
            }
        }
        S best = current;
        double bestFitness = currentFitness;

        int evaluations = mu;
        int iteration = 0;

        // Initial state
        RunState<S> initial = new RunState<>(iteration, evaluations, current, currentFitness, best, bestFitness, false);
        Observers.onStart(observers,initial, log);
        log.tick(initial.iteration(), initial.evaluations() - 1);
        Observers.onStep(observers,initial, log);

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
                    "parents", parents,
                    "parentsFitness", parentsFitness,
                    "generationSolutions", generationSolutions,
                    "generationFitness", generationFitness
            ));

            for (ScoutComponent component : components) {
                combinedStateVariables.putAll(component.getStateVariables(varState));
            }
            varState.update(combinedStateVariables);

            generationSolutions.clear();
            generationFitness.clear();

            // 3) Generate lambda children and evaluate them
            for (int k = 0; k < lambda; k++) {
                S child = generator.generate(rng);
                double childFitness = problem.fitness(child);

                evaluations++;
                generationSolutions.add(child);
                generationFitness.add(childFitness);
            }

            List<S> combinedSolutions = new ArrayList<>(parents.size() + generationSolutions.size());
            List<Double> combinedFitness = new ArrayList<>(parentsFitness.size() + generationFitness.size());
            combinedSolutions.addAll(parents);
            combinedSolutions.addAll(generationSolutions);
            combinedFitness.addAll(parentsFitness);
            combinedFitness.addAll(generationFitness);

            List<Integer> order = new ArrayList<>(combinedSolutions.size());
            for (int i = 0; i < combinedSolutions.size(); i++) {
                order.add(i);
            }
            order.sort((a, b) -> Double.compare(combinedFitness.get(b), combinedFitness.get(a)));

            List<S> nextParents = new ArrayList<>(mu);
            List<Double> nextParentsFitness = new ArrayList<>(mu);
            for (int i = 0; i < mu && i < order.size(); i++) {
                int idx = order.get(i);
                nextParents.add(combinedSolutions.get(idx));
                nextParentsFitness.add(combinedFitness.get(idx));
            }

            S bestOverall = nextParents.getFirst();
            double bestOverallFitness = nextParentsFitness.getFirst();

            boolean accepted = acceptance.accept(currentFitness, bestOverallFitness, iteration, rng);

            parents = nextParents;
            parentsFitness = nextParentsFitness;
            current = bestOverall;
            currentFitness = bestOverallFitness;

            if (currentFitness > bestFitness) {
                best = current;
                bestFitness = currentFitness;
            }

            RunState<S> stateLog = new RunState<>(iteration, evaluations, current, currentFitness, best, bestFitness, accepted);
            if ((stateLog.iteration() + 1) % logInterval == 0) {
                log.tick(stateLog.iteration(), stateLog.evaluations() - 1);
                Observers.onStep(observers,stateLog, log);
            }
            iteration++;
        }

        RunState<S> finalState = new RunState<>(iteration - 1, evaluations, current, currentFitness, best, bestFitness, false);
        if (((finalState.iteration() + 1) % logInterval) != 0) {
            log.tick(finalState.iteration(), finalState.evaluations() - 1);
            Observers.onStep(observers,finalState, log);
        }
        Observers.onEnd(observers,finalState, log);

        return log;
    }
}