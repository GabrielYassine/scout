package dk.dtu.scout.datatypes;

import dk.dtu.scout.util.DistanceUtils;

/**
 * Represents a TSP instance with city coordinates and a precomputed distance matrix.
 * The distance matrix is computed once during construction so later distance
 * lookups and tour length calculations can be performed efficiently.
 * @author s235257 & s230632
 */

public class TSPInstance {
    private final String name;
    private final String comment;
    private final int dimension;
    private final double[][] coordinates;
    private final double[][] distanceMatrix;

    public TSPInstance(String name, String comment, int dimension, double[][] coordinates) {
        this.name = name;
        this.comment = comment;
        this.dimension = dimension;
        this.coordinates = coordinates;
        this.distanceMatrix = computeDistanceMatrix();
    }
    /**
    * Computes the full pairwise distance matrix between all cities.
    * @return matrix where entry [i][j] is the distance from city i to city j
    * */
    private double[][] computeDistanceMatrix() {
        double[][] matrix = new double[dimension][dimension];
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                if (i == j) {
                    matrix[i][j] = 0.0;
                } else {
                    matrix[i][j] = DistanceUtils.euclideanRounded(coordinates[i], coordinates[j]);
                }
            }
        }
        return matrix;
    }

    public double getDistance(int cityA, int cityB) {
        return distanceMatrix[cityA][cityB];
    }
    /**
     * Calculates the total length of a complete TSP tour.
     * The tour is treated as cyclic, so the distance from the last city back
     * to the first city is included.
     * @param tour ordered list of city indices
     * @return total length of the tour
    */
    public double getTourLength(int[] tour) {
        double totalDistance = 0.0;
        for (int i = 0; i < tour.length - 1; i++) {
            totalDistance += getDistance(tour[i], tour[i + 1]);
        }
        totalDistance += getDistance(tour[tour.length - 1], tour[0]);
        return totalDistance;
    }

    public String getName() {
        return name;
    }

    public String getComment() {
        return comment;
    }

    public int getDimension() {
        return dimension;
    }

    public double[][] getCoordinates() {
        return coordinates;
    }
}