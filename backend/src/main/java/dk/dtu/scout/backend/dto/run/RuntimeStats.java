package dk.dtu.scout.backend.dto.run;

/**
 * Statistical summary of runtime/evaluation counts for a problem.
 */
public record RuntimeStats(
    int n,
    double mean,
    double variance,
    double stdDev,
    double ci95Low,
    double ci95High,
    double finalEvaluationsMedian
) {}
