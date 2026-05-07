package dk.dtu.scout.population;

import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.generator.Generator;
import dk.dtu.scout.logging.IterationSnapshot;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 *
 * @param <S>
 * @author s230632 & s235257
 */
@Component
@Scope("prototype")
public class MuLambdaPopulationModel<S> implements PopulationModel<S> {

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
            new Parameter("mu", "mu (Parents Amount)", "int", mu, 1.0, null, null),
            new Parameter("lambda", "lambda (Children Amount)", "int", lambda, 1.0, null, null)
        );
    }

    @Override
    public void configure(Map<String, Object> params) {
        mu = positiveIntParam(params, "mu", mu, "Mu must be positive");
        lambda = positiveIntParam(params, "lambda", lambda, "Lambda must be positive");
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

            if (parentFitness > currentFitness) {
                current = parent;
                currentFitness = parentFitness;
            }
        }

        S best = current;
        double bestFitness = currentFitness;
        int evaluations = mu;
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
            List<EvaluatedSolution<S>> chosen = context.parentSelection().selectParents(muState.parentsEvaluated, context.rng());

            S parent1 = chosen.get(0).value();
            S parent2 = chosen.get(1).value();

            context.sharedState().update(Map.of(StateKeys.SELECTED_PARENT_1, parent1, StateKeys.SELECTED_PARENT_2, parent2));

            S offspringBase = context.crossover() != null ? context.crossover().crossover(context.rng()) : parent1;

            context.sharedState().update(Map.of(StateKeys.OFFSPRING_BASE, offspringBase));

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

        muState.parentsEvaluated = nextParentsEvaluated;

        EvaluatedSolution<S> representative = bestOf(muState.parentsEvaluated);
        muState.current = representative.value();
        muState.currentFitness = representative.fitness();

        boolean accepted = previousCurrent != muState.current;

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

    private EvaluatedSolution<S> bestOf(List<EvaluatedSolution<S>> evaluatedSolutions) {
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
        return Map.of(
            StateKeys.CURRENT, current,
            StateKeys.BEST, best,
            StateKeys.BEST_FITNESS, bestFitness,
            StateKeys.CURRENT_FITNESS, currentFitness,
            StateKeys.PARENTS_EVALUATED, parentsEvaluated,
            StateKeys.GENERATION_EVALUATED, generationEvaluated
        );
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
}