package dk.dtu.scout.stopcondition;

public interface StopCondition<S> {
    boolean shouldStop(int iteration, int evaluations, double bestFitness, S bestSolution);
}