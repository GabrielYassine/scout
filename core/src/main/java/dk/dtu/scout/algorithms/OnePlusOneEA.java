package dk.dtu.scout.algorithms;

import dk.dtu.scout.datatypes.RunLog;
import dk.dtu.scout.problems.Problem;

import java.util.Random;

/**
 * Implementation of the (1+1) Evolutionary Algorithm for optimizing solutions of type S.
 * @param <S> The type of solutions being optimized.
 * @author s235257
 */

public class OnePlusOneEA<S> implements Algorithm {
    private final Problem<S> problem;
    private final int maxIterations;
    private final Random rng;
    private final RunLog runLog;

    public OnePlusOneEA(Problem<S> problem, int maxIterations, long seed, RunLog runLog) {
        this.problem = problem;
        this.maxIterations = maxIterations;
        this.rng = new Random(seed);
        this.runLog = runLog;
    }

    /**
     * We repeatedly mutate the current solution once per iteration, and accept the offspring if it is at least as good.
     * @author s235257
     */
    @Override
    public void run() {
        S current = problem.randomSolution();
        double currentFitness = problem.fitness(current);

        int iteration = 0;
        while (iteration < maxIterations && !problem.isOptimal(currentFitness)) {
            S offspring = mutate(current);
            double offspringFitness = problem.fitness(offspring);

            if (offspringFitness >= currentFitness) {
                current = offspring;
                currentFitness = offspringFitness;
            }
            runLog.log(iteration, currentFitness);
            iteration++;
        }
    }

    /**
     * Mutate the current solution by flipping a single random bit.
     * @param current The current solution.
     * @return The mutated solution.
     * @author s235257
     */
    private S mutate(S current) {
        if (current instanceof boolean[] bits) {
            double probability = 1.0 / bits.length;
            boolean[] mutated = bits.clone();
            if (mutated.length == 0) { // wont happen but just to be safe
                return current;
            }
            for (int i = 0; i < mutated.length; i++) {
                if (rng.nextDouble() < probability) {
                    mutated[i] = !mutated[i];
                }
            }
            return (S) mutated;
        }
        throw new UnsupportedOperationException("Mutation limited to boolean[] solutions.");
    }
}