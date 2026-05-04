package dk.dtu.scout.population;

import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.generator.Generator;
import dk.dtu.scout.logging.IterationSnapshot;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        if (params == null) {
            return;
        }
        this.mu = positiveIntParam(params, "mu", mu, "Mu must be positive");
        this.lambda = positiveIntParam(params, "lambda", lambda, "Lambda must be positive");
    }

    private static final class MuLambdaState<S> implements PopulationState<S> {
        private final Generator<S> generator;
        private List<EvaluatedSolution<S>> parentsEvaluated;
        private final List<EvaluatedSolution<S>> generationEvaluated;
        private S current;
        private double currentFitness;
        private S best;
        private double bestFitness;

        private MuLambdaState(
                Generator<S> generator,
                List<EvaluatedSolution<S>> parentsEvaluated,
                List<EvaluatedSolution<S>> generationEvaluated,
                S current,
                double currentFitness,
                S best,
                double bestFitness
        ) {
            this.generator = generator;
            this.parentsEvaluated = parentsEvaluated;
            this.generationEvaluated = generationEvaluated;
            this.current = current;
            this.currentFitness = currentFitness;
            this.best = best;
            this.bestFitness = bestFitness;
        }
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
                StateKeys.CURRENT, current,
                StateKeys.BEST, best,
                StateKeys.BEST_FITNESS, bestFitness,
                StateKeys.CURRENT_FITNESS, currentFitness,
                StateKeys.PARENTS_EVALUATED, parentsEvaluated,
                StateKeys.GENERATION_EVALUATED, generationEvaluated
        );
    }

    @Override
    public PopulationInitialization<S> initialize(PopulationModelContext<S> context) {
        Generator<S> generator = context.generatorFactory().get();
        generator.init(context.sharedState());

        List<EvaluatedSolution<S>> parentsEvaluated = new ArrayList<>(mu);

        S current = null;
        double currentFitness = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < mu; i++) {
            S parent = context.space().randomSolution(context.rng());
            double parentFitness = context.problem().fitness(parent);
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

        List<EvaluatedSolution<S>> generationEvaluated = new ArrayList<>();
        Map<String, Object> stateVariables = getPopulationStateVariables(
                current,
                best,
                bestFitness,
                currentFitness,
                parentsEvaluated,
                generationEvaluated
        );

        IterationSnapshot<S> initial = new IterationSnapshot<>(
                iteration,
                evaluations,
                new EvaluatedSolution<>(current, currentFitness),
                new EvaluatedSolution<>(best, bestFitness),
                true
        );

        MuLambdaState<S> state = new MuLambdaState<>(
                generator,
                parentsEvaluated,
                generationEvaluated,
                current,
                currentFitness,
                best,
                bestFitness
        );

        return new PopulationInitialization<>(state, initial, evaluations, stateVariables, List.of(generator));
    }

    @Override
    public PopulationStepResult<S> step(
            PopulationModelContext<S> context,
            PopulationState<S> state,
            int iteration,
            int evaluations
    ) {
        MuLambdaState<S> muState = (MuLambdaState<S>) state;
        muState.generationEvaluated.clear();

        int evaluationsDelta = 0;
        for (int k = 0; k < lambda; k++) {
            List<EvaluatedSolution<S>> chosen = context.parentSelection().selectParents(muState.parentsEvaluated,  context.rng());
            if (chosen == null || chosen.size() < 2) {
                throw new IllegalStateException("Parent selection returned fewer than 2 parents");
            }
            S parent1 = chosen.get(0).value();
            S parent2 = chosen.get(1).value();
            context.sharedState().update(Map.of(
                    StateKeys.SELECTED_PARENT_1, parent1,
                    StateKeys.SELECTED_PARENT_2, parent2
            ));
            S offspringBase;
            if (context.crossover() != null) {
                offspringBase = context.crossover().crossover(context.rng());
            } else {
                offspringBase = parent1;
            }
            context.sharedState().update(Map.of(
                    StateKeys.OFFSPRING_BASE, offspringBase
            ));
            S child = muState.generator.generate(context.rng());

            double childFitness = context.problem().fitness(child);
            evaluationsDelta++;
            muState.generationEvaluated.add(new EvaluatedSolution<>(child, childFitness));
        }

        S previousCurrent = muState.current;

        List<EvaluatedSolution<S>> nextParentsEvaluated = context.selection().select(
                muState.parentsEvaluated,
                muState.generationEvaluated,
                mu,
                iteration,
                context.rng()
        );

        if (nextParentsEvaluated == null || nextParentsEvaluated.isEmpty()) {
            throw new IllegalStateException("Selection rule returned no parents");
        }

        if (nextParentsEvaluated.size() != mu) {
            throw new IllegalStateException("Selection rule returned " + nextParentsEvaluated.size() + " parents, expected " + mu);
        }

        muState.parentsEvaluated = nextParentsEvaluated;

        // The representative shown to generic observers is the best individual
        // in the selected parent population. This is a framework choice; it is not
        // necessarily the only meaningful representative.
        EvaluatedSolution<S> representative = bestOf(muState.parentsEvaluated);
        muState.current = representative.value();
        muState.currentFitness = representative.fitness();

        // accepted should mean a new solution was accepted (representative changed),
        // not merely that the fitness was non-worsening.
        boolean accepted = previousCurrent != muState.current;

        // Global best-so-far must be updated separately from current,
        // because current may worsen under non-elitist selection.
        if (muState.currentFitness > muState.bestFitness) {
            muState.best = muState.current;
            muState.bestFitness = muState.currentFitness;
        }

        Map<String, Object> stateVariables = getPopulationStateVariables(
                muState.current,
                muState.best,
                muState.bestFitness,
                muState.currentFitness,
                muState.parentsEvaluated,
                muState.generationEvaluated
        );

        int newEvaluations = evaluations + evaluationsDelta;
        IterationSnapshot<S> runState = new IterationSnapshot<>(
                iteration,
                newEvaluations,
                new EvaluatedSolution<>(muState.current, muState.currentFitness),
                new EvaluatedSolution<>(muState.best, muState.bestFitness),
                accepted
        );

        return new PopulationStepResult<>(runState, evaluationsDelta, stateVariables);
    }
    private int positiveIntParam(Map<String, Object> params, String key, int currentValue, String errorMessage) {
        if (!params.containsKey(key)) {
            return currentValue;
        }

        int value = ((Number) params.get(key)).intValue();

        if (value <= 0) {
            throw new IllegalArgumentException(errorMessage);
        }

        return value;
    }
}
