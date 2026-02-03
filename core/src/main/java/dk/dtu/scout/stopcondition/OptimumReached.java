package dk.dtu.scout.stopcondition;

import dk.dtu.scout.problems.Problem;

public class OptimumReached<S> implements StopCondition<S> {
    private final Problem<S> problem;

    public OptimumReached(Problem<S> problem) {
        this.problem = problem;
    }

    @Override
    public boolean shouldStop(int iteration, int evaluations, double bestFitness, S bestSolution) {
        return problem.isOptimal(bestFitness);
    }
}
