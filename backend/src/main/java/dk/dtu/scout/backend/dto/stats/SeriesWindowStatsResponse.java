package dk.dtu.scout.backend.dto.stats;

public record SeriesWindowStatsResponse(
        String seriesName,
        String xAxisLabel,
        String yAxisLabel,
        double xMin,
        double xMax,
        int count,
        double min,
        double max,
        double mean,
        double stdDev,
        double median,
        double q1,
        double q3,
        double iqr,
        double slope,
        String trend
) {}
