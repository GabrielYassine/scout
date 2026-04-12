package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.stats.SeriesPoint;
import dk.dtu.scout.backend.dto.stats.SeriesWindowStatsRequest;
import dk.dtu.scout.backend.dto.stats.SeriesWindowStatsResponse;
import dk.dtu.scout.backend.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class StatsService {
    private static final double TREND_THRESHOLD = 0.0001;

    public SeriesWindowStatsResponse computeSeriesWindowStats(SeriesWindowStatsRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required.");
        }
        if (request.points() == null || request.points().isEmpty()) {
            throw new BadRequestException("points must not be empty.");
        }
        if (!Double.isFinite(request.xMin()) || !Double.isFinite(request.xMax())) {
            throw new BadRequestException("xMin and xMax must be finite numbers.");
        }
        if (request.xMin() > request.xMax()) {
            throw new BadRequestException("xMin must be less than or equal to xMax.");
        }

        List<SeriesPoint> filtered = request.points().stream()
                .filter(p -> p != null && Double.isFinite(p.x()) && Double.isFinite(p.y()))
                .filter(p -> p.x() >= request.xMin() && p.x() <= request.xMax())
                .sorted(Comparator.comparingDouble(SeriesPoint::x))
                .toList();

        if (filtered.isEmpty()) {
            throw new BadRequestException("No points fall inside the requested x-range.");
        }

        List<Double> ys = filtered.stream().map(SeriesPoint::y).sorted().toList();
        List<Double> xs = filtered.stream().map(SeriesPoint::x).toList();

        int count = ys.size();
        double mean = ys.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = ys.stream().mapToDouble(y -> (y - mean) * (y - mean)).average().orElse(0.0);
        double stdDev = Math.sqrt(variance);
        double min = ys.getFirst();
        double max = ys.getLast();
        double median = percentile(ys, 50);
        double q1 = percentile(ys, 25);
        double q3 = percentile(ys, 75);
        double iqr = q3 - q1;

        double xMean = xs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double covariance = 0.0;
        double xVariance = 0.0;
        for (SeriesPoint point : filtered) {
            double x = point.x();
            double y = point.y();
            covariance += (x - xMean) * (y - mean);
            xVariance += (x - xMean) * (x - xMean);
        }
        covariance /= count;
        xVariance /= count;
        double slope = xVariance == 0.0 ? 0.0 : covariance / xVariance;
        String trend = slope > TREND_THRESHOLD ? "up" : slope < -TREND_THRESHOLD ? "down" : "flat";

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

    private double percentile(List<Double> sorted, double p) {
        if (sorted == null || sorted.isEmpty()) return 0.0;
        if (sorted.size() == 1) return sorted.getFirst();

        double index = (p / 100.0) * (sorted.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) return sorted.get(lower);

        double fraction = index - lower;
        return sorted.get(lower) + fraction * (sorted.get(upper) - sorted.get(lower));
    }
}
