package dk.dtu.scout.datatypes;

import dk.dtu.scout.util.DistanceUtils;

/**
 * Represents a VRP instance with one depot, customer coordinates, demands,
 * vehicle capacity, and a precomputed distance matrix.
 * Node index 0 represents the depot. Customer nodes are indexed from 1 to
 * customerCount in the distance matrix.
 * @author s235257 & Ahmed
 */
public class VRPInstance {
    private final String name;
    private final String comment;
    private final int customerCount;
    private final double[][] customerCoordinates;
    private final double[] customerDemands;
    private final double[] depotCoordinates;
    private final double capacity;
    private final int numberOfVehicles;
    private final double[][] distanceMatrix;

    public VRPInstance(
        String name,
        String comment,
        double[] depotCoordinates,
        double[][] customerCoordinates,
        double[] customerDemands,
        double capacity,
        int numberOfVehicles
    ) {
        this.name = name;
        this.comment = comment;
        this.customerCoordinates = customerCoordinates;
        this.customerDemands = customerDemands;
        this.customerCount = customerCoordinates.length;
        this.depotCoordinates = depotCoordinates;
        this.capacity = capacity;
        this.numberOfVehicles = numberOfVehicles;
        this.distanceMatrix = computeDistanceMatrix();
    }
    /**
     * Computes the full pairwise distance matrix between the depot and all customers.
     * Node index 0 is the depot. Customer i is stored at node index i + 1.
     * @return matrix where entry [i][j] is the distance from node i to node j
     */
    private double[][] computeDistanceMatrix() {
        int size = customerCount + 1;
        double[][] matrix = new double[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i == j) {
                    matrix[i][j] = 0.0;
                } else {
                    matrix[i][j] = DistanceUtils.euclideanRounded(getCoordinates(i), getCoordinates(j));
                }
            }
        }

        return matrix;
    }

    private double[] getCoordinates(int nodeIndex) {
        return nodeIndex == 0 ? depotCoordinates : customerCoordinates[nodeIndex - 1];
    }

    public double getDistance(int nodeA, int nodeB) {
        return distanceMatrix[nodeA][nodeB];
    }

    public double getDemand(int customerIndex) {
        return customerDemands[customerIndex];
    }

    public int getCustomerCount() {
        return customerCount;
    }

    public String getName() {
        return name;
    }

    public String getComment() {
        return comment;
    }

    public double getCapacity() {
        return capacity;
    }

    public double[][] getCustomerCoordinates() {
        return customerCoordinates;
    }

    public double[] getDepotCoordinates() {
        return depotCoordinates;
    }

    public int getNumberOfVehicles() {
        return numberOfVehicles;
    }
}