package dk.dtu.scout.backend.util;

import dk.dtu.scout.backend.dto.stats.SeriesPoint;

import java.util.List;

/**
 * Utility class for statistical calculations.
 * @author s235257
 */
public final class StatisticsMath {

    private StatisticsMath() {
    }

    /**
     * Calculates the mean (average) of a list of double values.
     * @param values the list of double values to calculate the mean for
     * @return the mean of the values, or 0.0 if the input list is null or empty
     */
    public static double mean(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    /**
     * Calculates the variance of a list of double values given their mean.
     * @param values the list of double values to calculate the variance for
     * @param mean the mean of the values, which can be precomputed for efficiency
     * @return the variance of the values, or 0.0 if the input list is null or empty
     */
    public static double variance(List<Double> values, double mean) {
        return values.stream().mapToDouble(value -> (value - mean) * (value - mean)).average().orElse(0.0);
    }

    /**
     * Calculates the standard deviation of a list of double values given their mean.
     * @param values the list of double values to calculate the standard deviation for
     * @param mean the mean of the values, which can be precomputed for efficiency
     * @return the standard deviation of the values, or 0.0 if the input list is null or empty
     */
    public static double standardDeviation(List<Double> values, double mean) {
        return Math.sqrt(variance(values, mean));
    }

    /**
     * Calculates the p-th percentile of a sorted list of double values using linear interpolation.
     * @param sorted a sorted list of double values (must be sorted in ascending order)
     * @param p the percentile to calculate (between 0 and 100)
     * @return the p-th percentile value, or 0.0 if the input list is null or empty. If the list has only one element, that element is returned regardless of p.
     */
    public static double percentile(List<Double> sorted, double p) {

        if (sorted.size() == 1) {
            return sorted.getFirst();
        }

        double index = (p / 100.0) * (sorted.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);

        if (lower == upper) {
            return sorted.get(lower);
        }

        double fraction = index - lower;
        return sorted.get(lower) + fraction * (sorted.get(upper) - sorted.get(lower));
    }

    /**
     * Calculates the five-number summary (min, Q1, median, Q3, max) for a sorted list of values.
     * @param sorted a sorted list of double values
     * @return a list containing the five-number summary: [min, Q1, median, Q3, max]. Returns an empty list if input is null or empty.
     */
    public static List<Double> fiveNumberSummary(List<Double> sorted) {
        if (sorted.isEmpty()) {
            return List.of();
        }

        return List.of(
            sorted.getFirst(),
            percentile(sorted, 25),
            percentile(sorted, 50),
            percentile(sorted, 75),
            sorted.getLast()
        );
    }

    /**
     * Calculates the slope of the best fit line for the given points using the formula:
     * @param points the list of points to calculate the slope for
     * @param xMean the mean of the x-values of the points
     * @param yMean the mean of the y-values of the points
     * @return the slope of the best fit line, or 0.0 if the variance of x is zero or if points are null/empty
     */
    public static double slope(List<SeriesPoint> points, double xMean, double yMean) {
        double covariance = 0.0;
        double xVariance = 0.0;

        for (SeriesPoint point : points) {
            double xDifference = point.x() - xMean;
            double yDifference = point.y() - yMean;

            covariance += xDifference * yDifference;
            xVariance += xDifference * xDifference;
        }

        covariance /= points.size();
        xVariance /= points.size();

        return xVariance == 0.0 ? 0.0 : covariance / xVariance;
    }

    /**
     * Determines the trend ("up", "down", "flat") based on the slope and a given threshold.
     * @param slope the slope of the points
     * @param threshold the minimum absolute slope required to consider it an "up" or "down" trend; otherwise, it's "flat"
     * @return "up" if slope > threshold, "down" if slope < -threshold, "flat" otherwise
     */
    public static String trendFromSlope(double slope, double threshold) {
        if (slope > threshold) {
            return "up";
        }

        if (slope < -threshold) {
            return "down";
        }

        return "flat";
    }
}