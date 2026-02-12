package dk.dtu.scout.algorithms;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.acceptance.AcceptanceRule;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;
import dk.dtu.scout.mutation.Mutation;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.problems.Problem;

import java.util.List;
import java.util.Random;

/**
 * Implementation of the (1+1) Evolutionary Algorithm for optimizing solutions of type S.
 * @param <S> The type of solutions being optimized.
 * @author s235257
 */

public class OnePlusOneEA<S> implements Algorithm<S> {

    private final Mutation<S> mutation;
    private final AcceptanceRule acceptance;

    public OnePlusOneEA(Mutation<S> mutation, AcceptanceRule acceptance) {
        this.mutation = mutation;
        this.acceptance = acceptance;
    }

    @Override
    public String id() {
        return "1p1-ea";
    }

    @Override
    public String displayName() {
        return "(1+1) EA";
    }

    @Override
    public String description() {
        return "A simple evolutionary algorithm";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public RunLog run(Problem<S> problem, Random rng, int maxIterations, Observer<S> observer) {
        RunLog log = new RunLog();

        S current = problem.randomSolution(rng);
        double currentFitness = problem.fitness(current);

        S best = current;
        double bestFitness = currentFitness;

        int evaluations = 1;

        // Initial state
        RunState<S> initialState = new RunState<>(
            0, evaluations, current, currentFitness, best, bestFitness, false
        );
        observer.onStart(initialState, log);
        observer.onStep(initialState, log);

        int iteration = 1;
        while (iteration <= maxIterations && !problem.isOptimal(currentFitness)) {
            S offspring = mutation.mutate(current, rng);
            double offspringFitness = problem.fitness(offspring);
            evaluations++;

            boolean accepted = acceptance.accept(currentFitness, offspringFitness, iteration, rng);

            if (accepted) {
                current = offspring;
                currentFitness = offspringFitness;

                if (currentFitness > bestFitness) {
                    best = current;
                    bestFitness = currentFitness;
                }
            }

            RunState<S> state = new RunState<>(
                iteration, evaluations, current, currentFitness, best, bestFitness, accepted
            );
            observer.onStep(state, log);

            iteration++;
        }

        RunState<S> finalState = new RunState<>(
            iteration - 1, evaluations, current, currentFitness, best, bestFitness, false
        );
        observer.onEnd(finalState, log);

        return log;
    }
}