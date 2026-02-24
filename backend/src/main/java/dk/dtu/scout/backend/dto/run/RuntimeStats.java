package dk.dtu.scout.backend.dto.run;

public record RuntimeStats(
        int n,
        double mean,
        double variance,
        double stdDev,
        double ci95Low,
        double ci95High
) {}
