package dk.dtu.scout.stopcondition;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.problems.Problem;

import java.util.List;

public class OptimumReached<S> implements StopCondition<S> {
    private final Problem<S> problem;

    public OptimumReached(Problem<S> problem) {
        this.problem = problem;
    }

    @Override
    public String id() {
        return "optimum-reached";
    }

    @Override
    public String displayName() {
        return "Optimum Reached";
    }

    @Override
    public String description() {
        return "Stops when the optimal solution is found";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public boolean shouldStop(int iteration, int evaluations, double bestFitness, S bestSolution) {
        return problem.isOptimal(bestFitness);
    }
}
