package dk.dtu.scout.algorithms;

import dk.dtu.scout.acceptance.AcceptanceRule;
import dk.dtu.scout.datatypes.RunLog;
import dk.dtu.scout.mutation.Mutation;
import dk.dtu.scout.problems.Problem;

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
    public RunLog<S> run(Problem<S> problem, Random rng, int maxIterations) {
        RunLog<S> runLog = new RunLog<>();

        S current = problem.randomSolution(rng);
        double currentFitness = problem.fitness(current);

        runLog.log(0, currentFitness);

        int iteration = 1;
        while (iteration <= maxIterations && !problem.isOptimal(currentFitness)) {
            S offspring = mutation.mutate(current, rng);
            double offspringFitness = problem.fitness(offspring);

            if (acceptance.accept(currentFitness, offspringFitness, iteration, rng)) {
                current = offspring;
                currentFitness = offspringFitness;
            }

            runLog.log(iteration, currentFitness);
            iteration++;
        }

        runLog.setBestSolution(current);

        return runLog;
    }
}