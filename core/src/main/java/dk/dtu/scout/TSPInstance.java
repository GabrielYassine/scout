package dk.dtu.scout;

public class TSPInstance {
    private final String name;
    private final int dimension;
    private final double[][] coordinates;
    private final double[][] distanceMatrix;

    public TSPInstance(String name, int dimension, double[][] coordinates) {
        this.name = name;
        this.dimension = dimension;
        this.coordinates = coordinates;
        this.distanceMatrix = computeDistanceMatrix();
    }

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

    public int getDimension() {
        return dimension;
    }

    public double[][] getCoordinates() {
        return coordinates;
    }
}