package dk.dtu.scout;

public class VRPInstance {
    private final String name;
    private final int customerCount;
    private final double[][] customerCoordinates;
    private final double[] customerDemands;
    private final double[] depotCoordinates;
    private final double capacity;
    private final double[][] distanceMatrix;

    public VRPInstance(String name, double[] depotCoordinates, double[][] customerCoordinates, double[] customerDemands, double capacity) {
        this.name = name;
        this.customerCoordinates = customerCoordinates;
        this.customerDemands = customerDemands;
        this.customerCount = customerCoordinates.length;
        this.depotCoordinates = depotCoordinates;
        this.capacity = capacity;
        this.distanceMatrix = computeDistanceMatrix();
    }

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

    public double getCapacity() {
        return capacity;
    }

    public double[][] getCustomerCoordinates() {
        return customerCoordinates;
    }

    public double[] getDepotCoordinates() {
        return depotCoordinates;
    }
}
