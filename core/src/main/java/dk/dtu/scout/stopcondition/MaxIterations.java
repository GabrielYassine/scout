package dk.dtu.scout.stopcondition;

import dk.dtu.scout.Parameter;

import java.util.List;

public class MaxIterations<S> implements StopCondition<S> {
    private final int maxIterations;

    public MaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    @Override
    public String id() {
        return "max-iterations";
    }

    @Override
    public String displayName() {
        return "Max Iterations";
    }

    @Override
    public String description() {
        return "Stops the algorithm after a maximum number of iterations";
    }

    @Override
    public List<Parameter> params() {
        return List.of(
            new Parameter("maxIterations", "Max iterations", "int", 10_000, 1.0, null)
        );
    }

    @Override
    public boolean shouldStop(int iteration, int evaluations, double bestFitness, S bestSolution) {
        return iteration >= maxIterations;
    }
}