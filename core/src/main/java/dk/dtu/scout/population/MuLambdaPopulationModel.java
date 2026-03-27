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

    // mu = number of parents maintained between generations
    // lambda = number of offspring generated from the current state each iteration
    private int mu = 1;
    private int lambda = 1;

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
            SelectionRule<S> selection,
            SearchSpace<S> space,
            Problem<S> problem,
            List<StopCondition<S>> stopConditions,
            List<Observer<S>> observers
    ) {
        List<ScoutComponent> components = new ArrayList<>();
        components.add(generator);
        components.add(selection);
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

        // The population model does not assume the selection rule returns solutions sorted.
        // This keeps the contract for SelectionRule simpler and avoids hidden coupling.
        EvaluatedSolution<S> best = evaluatedSolutions.getFirst();
        for (int i = 1; i < evaluatedSolutions.size(); i++) {
            EvaluatedSolution<S> candidate = evaluatedSolutions.get(i);
            if (candidate.fitness() > best.fitness()) {
                best = candidate;
            }
        }
        return best;
    }

    private Map<String, Object> getPopulationStateVariables(
            S current,
            S best,
            double bestFitness,
            double currentFitness,
            List<EvaluatedSolution<S>> parentsEvaluated,
            List<EvaluatedSolution<S>> generationEvaluated
    ) {
        // current = representative of the current parent population
        // best = best solution ever seen during the whole run
        // These can differ for non-elitist or probabilistic selection rules.
        return Map.of(
                "current", current,
                "best", best,
                "bestFitness", bestFitness,
                "currentFitness", currentFitness,
                "parentsEvaluated", parentsEvaluated,
                "generationEvaluated", generationEvaluated
        );
    }

    private void updateComponentStateVariables(State varState, List<ScoutComponent> components) {
        Map<String, Object> combinedStateVariables = new HashMap<>();
        for (ScoutComponent component : components) {
            combinedStateVariables.putAll(component.getStateVariables(varState));
        }
        varState.update(combinedStateVariables);
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
        RunLog log = new RunLog();
        State varState = new State();

        if (logEveryIterations <= 0) {
            throw new IllegalArgumentException("logEveryIterations must be positive");
        }

        // A factory is used so population models that need multiple generators
        // later on (e.g. island models) can reuse the same run signature.
        Generator<S> generator = generatorFactory.get();

        List<ScoutComponent> components = initializeComponents(generator, selection, space, problem, stopConditions, observers);

        // Problem is exposed in the shared state before init() so dependent components
        // such as generators can inspect it during initialization.
        varState.update(Map.of("problem", problem));
        for (ScoutComponent component : components) {
            component.init(varState);
        }

        // Initial parents are sampled directly from the search space.
        // This makes the search space responsible for initialization,
        // while the generator is responsible for producing offspring afterwards.
        List<EvaluatedSolution<S>> parentsEvaluated = new ArrayList<>(mu);

        S current = null;
        double currentFitness = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < mu; i++) {
            S parent = space.randomSolution(rng);
            double parentFitness = problem.fitness(parent);
            parentsEvaluated.add(new EvaluatedSolution<>(parent, parentFitness));

            // current starts as the best representative among the initial parents.
            if (parentFitness > currentFitness) {
                currentFitness = parentFitness;
                current = parent;
            }
        }

        // best tracks best-so-far globally, not just the current representative.
        // This matters for non-elitist selection, where the population may later lose
        // its currently best-known solution.
        S best = current;
        double bestFitness = currentFitness;

        int evaluations = mu; // Starts at mu because the initial parents are evaluated before the first iteration.
        int iteration = 0;

        RunState<S> initial = new RunState<>(iteration, evaluations, current, currentFitness, best, bestFitness, false);
        Observers.onStart(observers, initial, log);
        log.tick(initial.iteration(), initial.evaluations() - 1);
        Observers.onStep(observers, initial, log);

        List<EvaluatedSolution<S>> generationEvaluated = new ArrayList<>();

        while (!StopConditions.shouldStop(stopConditions, iteration, evaluations, bestFitness, best)) {
            // Reuse the same list object to avoid unnecessary allocations each iteration.
            generationEvaluated.clear();

            // Pre-generation view: parents + empty current generation.
            varState.update(getPopulationStateVariables(
                    current,
                    best,
                    bestFitness,
                    currentFitness,
                    parentsEvaluated,
                    generationEvaluated
            ));
            updateComponentStateVariables(varState, components);

            // Generate and evaluate lambda offspring.
            // Evaluation count increases for each child, since fitness is computed here.
            for (int k = 0; k < lambda; k++) {
                S child = generator.generate(rng);
                double childFitness = problem.fitness(child);
                evaluations++;
                generationEvaluated.add(new EvaluatedSolution<>(child, childFitness));
            }

            double previousCurrentFitness = currentFitness;

            // The selection rule is responsible for survivor selection only.
            // It decides which evaluated solutions become the next parent population.
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

            // Returning more than mu would violate the fixed-size population model.
            if (nextParentsEvaluated.size() != mu) {
                throw new IllegalStateException("Selection rule returned " + nextParentsEvaluated.size() + " parents, expected " + mu);
            }

            parentsEvaluated = nextParentsEvaluated;

            // The representative shown to generic observers is the best individual
            // in the selected parent population. This is a framework choice; it is not
            // necessarily the only meaningful representative.
            EvaluatedSolution<S> representative = bestOf(parentsEvaluated);
            current = representative.value();
            currentFitness = representative.fitness();

            // accepted here no longer means pairwise accept/reject as in classical SA.
            // It now simply indicates whether the representative became no worse.
            boolean nonWorsening = currentFitness >= previousCurrentFitness;

            // Global best-so-far must be updated separately from current,
            // because current may worsen under non-elitist selection.
            if (currentFitness > bestFitness) {
                best = current;
                bestFitness = currentFitness;
            }

            // Post-selection view: new parents + current generation.
            varState.update(getPopulationStateVariables(
                    current,
                    best,
                    bestFitness,
                    currentFitness,
                    parentsEvaluated,
                    generationEvaluated
            ));
            updateComponentStateVariables(varState, components);

            RunState<S> stateLog = new RunState<>(iteration, evaluations, current, currentFitness, best, bestFitness, nonWorsening);

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