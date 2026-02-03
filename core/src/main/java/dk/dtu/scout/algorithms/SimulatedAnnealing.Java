package dk.dtu.scout.algorithms;

import dk.dtu.scout.acceptance.AcceptanceRule;
import dk.dtu.scout.datatypes.RunLog;
import dk.dtu.scout.mutation.Mutation;
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
    public RunLog<S> run(Problem<S> problem, Random rng, int maxIterations) {
        RunLog<S> runLog = new RunLog<>();

        S current = problem.randomSolution(rng);
        double currentFitness = problem.fitness(current);

        S best = current;
        double bestFitness = currentFitness;

        runLog.log(0, bestFitness);

        int iteration = 1;
        while (iteration <= maxIterations && !problem.isOptimal(bestFitness)) {

            S candidate = mutation.mutate(current, rng);
            double candidateFitness = problem.fitness(candidate);

            if (candidateFitness > bestFitness) {
                best = candidate;
                bestFitness = candidateFitness;
            }

            if (acceptance.accept(currentFitness, candidateFitness, iteration, rng)) {
                current = candidate;
                currentFitness = candidateFitness;
            }

            runLog.log(iteration, bestFitness);
            iteration++;
        }

        runLog.setBestSolution(best);
        runLog.setBestFitness(bestFitness);
        return runLog;
    }
}
