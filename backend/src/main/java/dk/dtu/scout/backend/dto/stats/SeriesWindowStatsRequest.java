package dk.dtu.scout.backend.dto.stats;

import java.util.List;

/**
 * DTO for a request to generate statistics for a data series, containing the series name, axis labels, axis limits, and data points.
 * @param seriesName the name of the data series
 * @param xAxisLabel the label for the x-axis
 * @param yAxisLabel the label for the y-axis
 * @param xMin the minimum value for the x-axis
 * @param xMax the maximum value for the x-axis
 * @param points the data points in the series
 * @author s235257 & Ahmed
 */
public record SeriesWindowStatsRequest(
    String seriesName,
    String xAxisLabel,
    String yAxisLabel,
    double xMin,
    double xMax,
    List<SeriesPoint> points
) {}
