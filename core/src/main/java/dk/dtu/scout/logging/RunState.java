package dk.dtu.scout.logging;

public record RunState<S> (
    int iteration,
    int evaluations,
    S currentSolution,
    double currentFitness,
    S bestSolution,
    double bestFitness,
    boolean accepted
) {}
