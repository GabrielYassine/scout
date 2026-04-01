package dk.dtu.scout;

/**
 * Pairing of a solution with its fitness value.
 */
public record EvaluatedSolution<S>(
    S value,
    double fitness
) {}
