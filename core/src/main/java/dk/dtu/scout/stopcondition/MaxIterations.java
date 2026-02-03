package dk.dtu.scout.stopcondition;

public class MaxIterations<S> implements StopCondition<S> {
    private final int maxIterations;

    public MaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    @Override
    public boolean shouldStop(int iteration, int evaluations, double bestFitness, S bestSolution) {
        return iteration >= maxIterations;
    }
}