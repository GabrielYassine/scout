package dk.dtu.scout.algorithms;

import dk.dtu.scout.acceptance.AcceptanceRule;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;
import dk.dtu.scout.mutation.Mutation;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.problems.Problem;

import java.util.Random;

/**
 * Simulated Annealing algorithm using a mutation (neighborhood) and an acceptance rule.
 * @param <S> The type of solutions being optimized.
 *   * @author s230632
 */
public class SimulatedAnnealing<S> implements Algorithm<S> {

    private final Mutation<S> mutation;
    private final AcceptanceRule acceptance;

    public SimulatedAnnealing(Mutation<S> mutation, AcceptanceRule acceptance) {
        this.mutation = mutation;
        this.acceptance = acceptance;
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
        while (iteration <= maxIterations && !problem.isOptimal(bestFitness)) {

            S candidate = mutation.mutate(current, rng);
            double candidateFitness = problem.fitness(candidate);
            evaluations++;

            if (candidateFitness > bestFitness) {
                best = candidate;
                bestFitness = candidateFitness;
            }

            boolean accepted = acceptance.accept(currentFitness, candidateFitness, iteration, rng);

            if (accepted) {
                current = candidate;
                currentFitness = candidateFitness;
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
