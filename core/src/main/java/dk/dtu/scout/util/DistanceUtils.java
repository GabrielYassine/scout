package dk.dtu.scout.util;

/**
 * Utility class for distance calculations.
 * @author s235257
 */
public final class DistanceUtils {
    private DistanceUtils() {}
    /**
     * Calculates the Euclidean distance between two points and rounds it to the nearest integer.
     * @param p1 First point as an array of [x, y]
     * @param p2 Second point as an array of [x, y]
     * @return Rounded Euclidean distance between p1 and p2
     */
    public static double euclideanRounded(double[] p1, double[] p2) {
        double dx = p1[0] - p2[0];
        double dy = p1[1] - p2[1];
        return Math.round(Math.sqrt(dx * dx + dy * dy));
    }
}
