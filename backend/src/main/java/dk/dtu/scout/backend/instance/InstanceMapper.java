package dk.dtu.scout.backend.instance;

import dk.dtu.scout.datatypes.TSPInstance;
import dk.dtu.scout.datatypes.VRPInstance;

import java.util.List;
import java.util.Map;

/**
 * Mapper for converting between raw map representations of TSP and VRP instances
 * and the internal TSPInstance and VRPInstance data types.
 * It validates required structure and simple semantic constraints while mapping.
 * @author s235257
 */
public final class InstanceMapper {

    private InstanceMapper() {
    }

    /**
     * Converts a map representing a TSP instance into a TSPInstance object.
     * @param map the map containing TSP instance data.
     * @return a TSPInstance object constructed from the provided map.
     */
    public static TSPInstance toTspInstance(Map<String, Object> map) {
        String name = map.getOrDefault("name", "Custom TSP Instance").toString();
        String comment = map.getOrDefault("comment", "").toString();
        Object citiesObj = map.get("cities");

        if (!(citiesObj instanceof List<?> citiesList) || citiesList.isEmpty()) {
            throw new IllegalArgumentException("tspInstance must contain a non-empty cities list");
        }

        double[][] coordinates = new double[citiesList.size()][2];

        for (int i = 0; i < citiesList.size(); i++) {
            Object cityObj = citiesList.get(i);
            if (!(cityObj instanceof Map<?, ?> cityRaw)) {
                throw new IllegalArgumentException("tspInstance city must be a map");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> city = (Map<String, Object>) cityRaw;

            coordinates[i][0] = toDouble(city.get("x"));
            coordinates[i][1] = toDouble(city.get("y"));
        }

        return new TSPInstance(name, comment, coordinates.length, coordinates);
    }

    /**
     * Converts a map representing a VRP instance into a VRPInstance object.
     * @param map the map containing VRP instance data.
     * @return a VRPInstance object constructed from the provided map.
     */
    public static VRPInstance toVrpInstance(Map<String, Object> map) {
        String name = map.getOrDefault("name", "Custom VRP Instance").toString();
        String comment = map.getOrDefault("comment", "").toString();

        double capacity = toDouble(map.get("capacity"));
        if (capacity < 0) {
            throw new IllegalArgumentException("VRP capacity cannot be negative");
        }

        int numberOfVehicles = toInt(map.getOrDefault("numberOfVehicles", 1));
        if (numberOfVehicles <= 0) {
            throw new IllegalArgumentException("Number of vehicles must be positive");
        }

        double[] depotCoordinates = extractDepot(map);

        Object customersObj = map.get("customers");
        if (!(customersObj instanceof List<?> customersList) || customersList.isEmpty()) {
            throw new IllegalArgumentException("vrpInstance must contain a non-empty customers list");
        }

        double[][] customerCoordinates = new double[customersList.size()][2];
        double[] customerDemands = new double[customersList.size()];

        for (int i = 0; i < customersList.size(); i++) {
            Object customerObj = customersList.get(i);
            if (!(customerObj instanceof Map<?, ?> customerRaw)) {
                throw new IllegalArgumentException("vrpInstance customer must be a map");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> customer = (Map<String, Object>) customerRaw;

            customerCoordinates[i][0] = toDouble(customer.get("x"));
            customerCoordinates[i][1] = toDouble(customer.get("y"));

            double demand = toDouble(customer.get("demand"));
            if (demand < 0) {
                throw new IllegalArgumentException("VRP customer " + (i + 1) + " demand cannot be negative");
            }

            if (demand > capacity) {
                throw new IllegalArgumentException("VRP customer " + (i + 1) + " demand exceeds vehicle capacity");
            }

            customerDemands[i] = demand;
        }

        return new VRPInstance(
                name,
                comment,
                depotCoordinates,
                customerCoordinates,
                customerDemands,
                capacity,
                numberOfVehicles
        );
    }

    /**
     * Extracts depot coordinates from the provided map.
     * @param map the map containing the VRP instance data.
     * @return a double array of length 2 containing the x and y coordinates of the depot.
     */
    private static double[] extractDepot(Map<String, Object> map) {
        Object depotObj = map.get("depot");
        if (!(depotObj instanceof Map<?, ?> depotRaw)) {
            throw new IllegalArgumentException("vrpInstance must include depot coordinates");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> depot = (Map<String, Object>) depotRaw;

        return new double[] {toDouble(depot.get("x")), toDouble(depot.get("y"))};
    }

    private static double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }

        if (value == null) {
            throw new IllegalArgumentException("Missing numeric value");
        }

        return Double.parseDouble(value.toString());
    }

    private static int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }

        if (value == null) {
            throw new IllegalArgumentException("Missing integer value");
        }

        return Integer.parseInt(value.toString());
    }
}