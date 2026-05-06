package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.stats.SeriesPoint;
import dk.dtu.scout.backend.dto.stats.SeriesWindowStatsRequest;
import dk.dtu.scout.backend.dto.stats.SeriesWindowStatsResponse;
import dk.dtu.scout.backend.exception.BadRequestException;
import dk.dtu.scout.backend.util.StatisticsMath;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Computes statistics for a series of (x,y) points within a specified x-range.
 * @author s235257
 */
@Service
public class SeriesStatsService {

    private static final double TREND_THRESHOLD = 0.0001;

    /**
     * Computes count, min, max, mean, std dev, median, quartiles, IQR, and trend for points in the specified x-range.
     * @param request the request containing the series points, x-range, and labels
     * @return the computed statistics for the points in the x-range
     */
    public SeriesWindowStatsResponse computeSeriesWindowStats(SeriesWindowStatsRequest request) {
        validateRequest(request);

        List<SeriesPoint> filtered = filterPointsInRange(request);

        if (filtered.isEmpty()) {
            throw new BadRequestException("No points fall inside the requested x-range.");
        }

        List<Double> ys = filtered.stream().map(SeriesPoint::y).sorted().toList();
        List<Double> xs = filtered.stream().map(SeriesPoint::x).toList();
        int count = ys.size();

        double mean = StatisticsMath.mean(ys);
        double stdDev = StatisticsMath.standardDeviation(ys, mean);

        double min = ys.getFirst();
        double max = ys.getLast();
        double median = StatisticsMath.percentile(ys, 50);
        double q1 = StatisticsMath.percentile(ys, 25);
        double q3 = StatisticsMath.percentile(ys, 75);
        double iqr = q3 - q1;

        double xMean = StatisticsMath.mean(xs);
        double slope = StatisticsMath.slope(filtered, xMean, mean);
        String trend = StatisticsMath.trendFromSlope(slope, TREND_THRESHOLD);

        return new SeriesWindowStatsResponse(
            request.seriesName(),
            request.xAxisLabel(),
            request.yAxisLabel(),
            request.xMin(),
            request.xMax(),
            count,
            min,
            max,
            mean,
            stdDev,
            median,
            q1,
            q3,
            iqr,
            slope,
            trend
        );
    }

    /**
     * Safety filter to ensure we only consider valid points within the specified x-range, and sort them by x-value.
     * @param request the request containing the series points and x-range
     * @return the filtered and sorted list of points that fall within the x-range
     */
    private List<SeriesPoint> filterPointsInRange(SeriesWindowStatsRequest request) {
        return request.points().stream()
            .filter(point -> point != null && Double.isFinite(point.x()) && Double.isFinite(point.y()))
            .filter(point -> point.x() >= request.xMin() && point.x() <= request.xMax())
            .sorted(Comparator.comparingDouble(SeriesPoint::x))
            .toList();
    }

    private void validateRequest(SeriesWindowStatsRequest request) {
        if (request.points() == null || request.points().isEmpty()) {
            throw new BadRequestException("points must not be empty.");
        }

        if (request.xMin() > request.xMax()) {
            throw new BadRequestException("xMin must be less than or equal to xMax.");
        }
    }
}