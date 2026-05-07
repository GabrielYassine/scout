package dk.dtu.scout.logging;

import dk.dtu.scout.dto.EvaluatedSolution;

/**
 * Immutable snapshot of the algorithm state at a specific iteration.
 * @param iteration current algorithm iteration
 * @param evaluations number of fitness evaluations performed so far
 * @param current current representative solution
 * @param best best solution found so far
 * @param accepted whether the current step accepted a new solution
 * @param <S> solution representation type
 * @author s235257 & s230632
 */
public record IterationSnapshot<S>(
    int iteration,
    int evaluations,
    EvaluatedSolution<S> current,
    EvaluatedSolution<S> best,
    boolean accepted
) {
    public S currentSolution() {
        return current == null ? null : current.value();
    }

    public double currentFitness() {
        return current == null ? Double.NaN : current.fitness();
    }

    public S bestSolution() {
        return best == null ? null : best.value();
    }

    public double bestFitness() {
        return best == null ? Double.NaN : best.fitness();
    }
}
