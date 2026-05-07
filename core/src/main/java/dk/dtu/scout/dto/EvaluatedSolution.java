package dk.dtu.scout.dto;

/**
 * Pairing of a solution with its fitness value.
 * @author s235257 & Ahmed
 */
public record EvaluatedSolution<S>(
    S value,
    double fitness
) {}
