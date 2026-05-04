package dk.dtu.scout.backend.instance;

import dk.dtu.scout.datatypes.TSPInstance;
import dk.dtu.scout.datatypes.VRPInstance;

/**
 * Validator for TSP and VRP instances.
 * Ensures that instances contain semantically valid dimensions, capacities, demands,
 * and finite numeric values before they are used by the backend.
 * @author s235257
 */
public final class InstanceValidator {

    private InstanceValidator() {
    }

    /**
     * Validates a TSP instance by checking its dimension and coordinate values.
     * Structural validity is expected to be guaranteed by the mapper and instance constructor.
     * @param instance the TSP instance to validate
     */
    public static void validateTsp(TSPInstance instance) {
        if (instance.getDimension() <= 0) {
            throw new IllegalArgumentException("TSP instance must contain at least one city");
        }

        double[][] coordinates = instance.getCoordinates();

        for (int i = 0; i < coordinates.length; i++) {
            validateFinite(coordinates[i][0]);
            validateFinite(coordinates[i][1]);
        }
    }

    /**
     * Validates a VRP instance by checking vehicles, capacity, coordinates, and demands.
     * Structural validity is expected to be guaranteed by the mapper and instance constructor.
     * @param instance the VRP instance to validate
     */
    public static void validateVrp(VRPInstance instance) {
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
        validateFinite(depot[0]);
        validateFinite(depot[1]);

        double[][] customers = instance.getCustomerCoordinates();

        for (int i = 0; i < instance.getCustomerCount(); i++) {
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