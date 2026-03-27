package dk.dtu.scout;

public record EvaluatedSolution<S>(
    S value,
    double fitness
) {}
