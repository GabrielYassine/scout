package dk.dtu.scout.backend.dto.stats;

import java.util.List;

public record SeriesWindowStatsRequest(
        String seriesName,
        String xAxisLabel,
        String yAxisLabel,
        double xMin,
        double xMax,
        List<SeriesPoint> points
) {}
