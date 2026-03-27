package dk.dtu.scout.population;
import dk.dtu.scout.EvaluatedSolution;
import dk.dtu.scout.Parameter;
import dk.dtu.scout.ScoutComponent;
import dk.dtu.scout.State;
import dk.dtu.scout.acceptance.SelectionRule;
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
public class MuLambdaPopulationModel<S> implements PopulationModel<S> {

    private int mu = 1; // parents, default 1
    private int lambda = 1; // children, default 1

    @Override
    public String id() {
        return "mu-lambda";
    }

    @Override
    public String displayName() {
        return "Mu-Lambda Population Model";
    }

    @Override
    public String description() {
        return "Population model that evolves mu parents with lambda offspring";
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
            SelectionRule<S> acceptance,
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

    private EvaluatedSolution<S> bestOf(List<EvaluatedSolution<S>> evaluatedSolutions) {
        if (evaluatedSolutions == null || evaluatedSolutions.isEmpty()) {
            throw new IllegalStateException("No evaluated solutions available");
        }

        EvaluatedSolution<S> best = evaluatedSolutions.get(0);
        for (int i = 1; i < evaluatedSolutions.size(); i++) {
            EvaluatedSolution<S> candidate = evaluatedSolutions.get(i);
            if (candidate.fitness() > best.fitness()) {
                best = candidate;
            }
        }
        return best;
    }

    @Override
    public RunLog run(
            Supplier<Generator<S>> generatorFactory,
            SelectionRule<S> selection,
            SearchSpace<S> space,
            Problem<S> problem,
            Random rng,
            List<StopCondition<S>> stopConditions,
            List<Observer<S>> observers,
            int logEveryIterations
    ) {
        // Initialize log and state
        RunLog log = new RunLog();
        State varState = new State();

        // Validate logEveryIterations
        if (logEveryIterations <= 0) {
            throw new IllegalArgumentException("logEveryIterations must be positive");
        }

        // Island model needs a factory for each island
        Generator<S> generator = generatorFactory.get();

        // Initialize components list (MOVE OUTSIDE)
        List<ScoutComponent> components = initializeComponents(generator, selection, space, problem, stopConditions, observers);

        // Store problem in state so generators can access it
        varState.update(Map.of("problem", problem));

        // Initialize all components with state (MOVE OUTSIDE)
        for (ScoutComponent component : components) {
            component.init(varState);
        }

        // 1) Initialize parents
        List<EvaluatedSolution<S>> parentsEvaluated = new ArrayList<>(mu);

        // Set default values for current
        S current = null;
        double currentFitness = Double.NEGATIVE_INFINITY;

        // Generate initial parents and evaluate them
        for (int i = 0; i < mu; i++) {
            S parent = space.randomSolution(rng);
            double parentFitness = problem.fitness(parent);
            parentsEvaluated.add(new EvaluatedSolution<>(parent, parentFitness));

            // Find best among initial parents to set as current
            if (parentFitness > currentFitness) {
                currentFitness = parentFitness;
                current = parent;
            }
        }

        // Set the best solution as the best among initial parents
        S best = current;
        double bestFitness = currentFitness;

        int evaluations = mu; // We have evaluated mu parents so far
        int iteration = 0;

        // Initial state
        RunState<S> initial = new RunState<>(iteration, evaluations, current, currentFitness, best, bestFitness, false);
        Observers.onStart(observers,initial, log);
        log.tick(initial.iteration(), initial.evaluations() - 1);
        Observers.onStep(observers,initial, log);

        // Temporary lists to hold generation solutions and fitness for state variables
        List<EvaluatedSolution<S>> generationEvaluated = new ArrayList<>();

        // 2) Loop until stop condition is met
        while (!StopConditions.shouldStop(stopConditions, iteration, evaluations, bestFitness, best)) {
            // Update state variables from all components first
            Map<String, Object> combinedStateVariables = new HashMap<>();
            // Order of state variables in the map (for consistency and readability):
            // 1) population model
            // 1) generator
            // 2) selection
            // 3) space
            // 4) problem
            // 5) stop
            // 6) observer
            varState.update(Map.of(
                    "current", current,
                    "best", best,
                    "bestFitness", bestFitness,
                    "currentFitness", currentFitness,
                    "parentsEvaluated", parentsEvaluated,
                    "generationEvaluated", generationEvaluated
            ));

            // Should make sure that all vars have been initialized before calling getStateVariables (FIX)
            for (ScoutComponent component : components) {
                combinedStateVariables.putAll(component.getStateVariables(varState));
            }
            varState.update(combinedStateVariables);

            // Clear generation lists for the new generation
            generationEvaluated.clear();

            // 3) Generate lambda children and evaluate them
            for (int k = 0; k < lambda; k++) {
                S child = generator.generate(rng);
                double childFitness = problem.fitness(child);
                evaluations++;
                generationEvaluated.add(new EvaluatedSolution<>(child, childFitness));
            }

            double previousCurrentFitness = currentFitness;

            List<EvaluatedSolution<S>> nextParentsEvaluated = selection.select(
                parentsEvaluated,
                generationEvaluated,
                mu,
                iteration,
                rng
            );

            if (nextParentsEvaluated == null || nextParentsEvaluated.isEmpty()) {
                throw new IllegalStateException("Selection rule returned no parents");
            }

            if (nextParentsEvaluated.size() > mu) {
                throw new IllegalStateException("Selection rule returned more parents than mu: " + nextParentsEvaluated.size());
            }

            parentsEvaluated = nextParentsEvaluated;

            EvaluatedSolution<S> representative = bestOf(parentsEvaluated);
            current = representative.value();
            currentFitness = representative.fitness();

            boolean accepted = currentFitness >= previousCurrentFitness;

            if (currentFitness > bestFitness) {
                best = current;
                bestFitness = currentFitness;
            }

            RunState<S> stateLog = new RunState<>(iteration, evaluations, current, currentFitness, best, bestFitness, accepted);

            if ((stateLog.iteration() + 1) % logEveryIterations == 0) {
                log.tick(stateLog.iteration(), stateLog.evaluations() - 1);
                Observers.onStep(observers, stateLog, log);
            }
            iteration++;
        }

        RunState<S> finalState = new RunState<>(iteration - 1, evaluations, current, currentFitness, best, bestFitness, false);

        if (((finalState.iteration() + 1) % logEveryIterations) != 0) {
            log.tick(finalState.iteration(), finalState.evaluations() - 1);
            Observers.onStep(observers, finalState, log);
        }

        Observers.onEnd(observers, finalState, log);
        return log;
    }
}