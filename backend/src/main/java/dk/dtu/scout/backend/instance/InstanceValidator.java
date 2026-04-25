package dk.dtu.scout.backend.instance;

import dk.dtu.scout.datatypes.TSPInstance;
import dk.dtu.scout.datatypes.VRPInstance;

/**
 * Validator for TSP and VRP instances.
 * Ensures that instances contain valid dimensions, coordinates, demands, and capacities, and that all numeric values are finite.
 * @author s235257
 */
public final class InstanceValidator {

    private InstanceValidator() {
    }

    /**
     * Validates a TSP instance,
     * ensuring it contains a valid dimension, coordinates for each city, and that all coordinate values are finite numbers.
     * @param instance the TSP instance to validate
     */
    public static void validateTsp(TSPInstance instance) {
        if (instance == null) {
            throw new IllegalArgumentException("TSP instance must be provided");
        }

        if (instance.getDimension() <= 0) {
            throw new IllegalArgumentException("TSP instance must contain at least one city");
        }

        double[][] coordinates = instance.getCoordinates();
        if (coordinates == null || coordinates.length != instance.getDimension()) {
            throw new IllegalArgumentException("TSP coordinates must match dimension");
        }

        for (int i = 0; i < coordinates.length; i++) {
            if (coordinates[i] == null || coordinates[i].length < 2) {
                throw new IllegalArgumentException("TSP city " + (i + 1) + " must contain x and y coordinates");
            }

            validateFinite(coordinates[i][0]);
            validateFinite(coordinates[i][1]);
        }
    }

    /**
     * Validates a VRP instance,
     * ensuring it contains a valid number of customers, vehicles, capacity, depot coordinates, and customer coordinates and demands.
     * @param instance the VRP instance to validate
     */
    public static void validateVrp(VRPInstance instance) {
        if (instance == null) {
            throw new IllegalArgumentException("VRP instance must be provided");
        }

        if (instance.getCustomerCount() <= 0) {
            throw new IllegalArgumentException("VRP instance must contain at least one customer");
        }

        if (instance.getNumberOfVehicles() <= 0) {
            throw new IllegalArgumentException("VRP number of vehicles must be positive");
        }

        if (instance.getCapacity() < 0) {
            throw new IllegalArgumentException("VRP capacity cannot be negative");
        }

        double[] depot = instance.getDepotCoordinates();
        if (depot == null || depot.length < 2) {
            throw new IllegalArgumentException("VRP instance must contain depot coordinates");
        }

        validateFinite(depot[0]);
        validateFinite(depot[1]);

        double[][] customers = instance.getCustomerCoordinates();
        if (customers == null || customers.length != instance.getCustomerCount()) {
            throw new IllegalArgumentException("VRP customer coordinates must match customer count");
        }

        for (int i = 0; i < instance.getCustomerCount(); i++) {
            if (customers[i] == null || customers[i].length < 2) {
                throw new IllegalArgumentException("VRP customer " + (i + 1) + " must contain x and y coordinates");
            }

            validateFinite(customers[i][0]);
            validateFinite(customers[i][1]);

            double demand = instance.getDemand(i);
            validateFinite(demand);

            if (demand < 0) {
                throw new IllegalArgumentException("VRP customer " + (i + 1) + " demand cannot be negative");
            }

            if (demand > instance.getCapacity()) {
                throw new IllegalArgumentException("VRP customer " + (i + 1) + " demand exceeds vehicle capacity");
            }
        }
    }

    /**
     * Ensures that a numeric value is finite, meaning it is not NaN or infinite.
     * @param value the value to validate
     */
    private static void validateFinite(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Instance contains a non-finite numeric value");
        }
    }
}