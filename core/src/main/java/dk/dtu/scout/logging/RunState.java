package dk.dtu.scout.logging;

/**
 * Immutable snapshot of the algorithm state at a given iteration.
 */
public record RunState<S> (
    int iteration,
    int evaluations,
    S currentSolution,
    double currentFitness,
    S bestSolution,
    double bestFitness,
    boolean accepted
) {}
