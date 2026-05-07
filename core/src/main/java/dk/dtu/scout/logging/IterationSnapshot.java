package dk.dtu.scout.logging;

import dk.dtu.scout.dto.EvaluatedSolution;

/**
 * Immutable snapshot of the algorithm state at a given iteration.
 * @author s235257 & Ahmed
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
