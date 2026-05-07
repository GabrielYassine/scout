package dk.dtu.scout.dto;

/**
 * Pairing of a solution with its fitness value.
 * @param value the solution representation
 * @param fitness the fitness value assigned to the solution
 * @param <S> the solution representation type
 * @author s235257 & s230632
 */
public record EvaluatedSolution<S>(
    S value,
    double fitness
) {}
