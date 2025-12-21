package dk.dtu.scout.problems;

public interface Problem<S> {
    S randomSolution();
    double fitness(S solution);
    boolean isOptimal(double fitness);
}
