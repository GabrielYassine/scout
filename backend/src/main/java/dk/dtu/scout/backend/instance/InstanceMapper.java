package dk.dtu.scout.backend.instance;

import dk.dtu.scout.datatypes.TSPInstance;
import dk.dtu.scout.datatypes.VRPInstance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapper for converting between raw map representations of TSP and VRP instances
 * and the internal TSPInstance and VRPInstance data types.
 * It is also responsible for converting TSPInstance and VRPInstance objects back into map format suitable for frontend use and export.
 * @author s235257
 */
public final class InstanceMapper {

    private static final String EDGE_WEIGHT_TYPE = "EUC_2D";

    private InstanceMapper() {
    }

    /**
     * Converts a map representing a TSP instance into a TSPInstance object.
     * @param map the map containing TSP instance data.
     * @return a TSPInstance object constructed from the provided map.
     */
    public static TSPInstance toTspInstance(Map<String, Object> map) {
        if (map == null) {
            throw new IllegalArgumentException("tspInstance must be provided");
        }

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

            @SuppressWarnings("unchecked") // Safe as JSON uses string keys and object values.
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
        if (map == null) {
            throw new IllegalArgumentException("vrpInstance must be provided");
        }

        String name = map.getOrDefault("name", "Custom VRP Instance").toString();
        String comment = map.getOrDefault("comment", "").toString();
        double capacity = toDouble(map.get("capacity"));
        int numberOfVehicles = toInt(map.get("numberOfVehicles"));

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

            @SuppressWarnings("unchecked") // Safe as JSON uses string keys and object values.
            Map<String, Object> customer = (Map<String, Object>) customerRaw;

            customerCoordinates[i][0] = toDouble(customer.get("x"));
            customerCoordinates[i][1] = toDouble(customer.get("y"));
            customerDemands[i] = toDouble(customer.get("demand"));
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
     * Converts a TSPInstance object into a map format suitable for frontend use and export.
     * @param instance the TSPInstance to convert into a payload map.
     * @return a map containing the TSP instance data in a structured format for frontend display and export.
     */
    public static Map<String, Object> toTspPayload(TSPInstance instance) {
        List<Map<String, Object>> cities = new ArrayList<>();
        double[][] coordinates = instance.getCoordinates();

        for (int i = 0; i < coordinates.length; i++) {
            Map<String, Object> city = new LinkedHashMap<>();
            city.put("id", i);
            city.put("x", coordinates[i][0]);
            city.put("y", coordinates[i][1]);
            cities.add(city);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", instance.getName());
        payload.put("comment", instance.getComment());
        payload.put("type", "TSP");
        payload.put("dimension", instance.getDimension());
        payload.put("edgeWeightType", EDGE_WEIGHT_TYPE);
        payload.put("cities", cities);

        return payload;
    }

    /**
     * Converts a VRPInstance object into a map format suitable for frontend use and export.
     * @param instance the VRPInstance to convert into a payload map.
     * @return a map containing the VRP instance data in a structured format for frontend display and export.
     */
    public static Map<String, Object> toVrpPayload(VRPInstance instance) {
        double[] depotCoordinates = instance.getDepotCoordinates();

        Map<String, Object> depot = new LinkedHashMap<>();
        depot.put("x", depotCoordinates[0]);
        depot.put("y", depotCoordinates[1]);

        Map<String, Object> depotNode = new LinkedHashMap<>();
        depotNode.put("id", 0);
        depotNode.put("nodeId", 1);
        depotNode.put("x", depotCoordinates[0]);
        depotNode.put("y", depotCoordinates[1]);

        List<Map<String, Object>> customers = new ArrayList<>();
        double[][] customerCoordinates = instance.getCustomerCoordinates();

        for (int i = 0; i < customerCoordinates.length; i++) {
            Map<String, Object> customer = new LinkedHashMap<>();
            customer.put("id", i);
            customer.put("x", customerCoordinates[i][0]);
            customer.put("y", customerCoordinates[i][1]);
            customer.put("demand", instance.getDemand(i));
            customer.put("originalId", i + 2);
            customers.add(customer);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", instance.getName());
        payload.put("comment", instance.getComment());
        payload.put("type", "CVRP");
        payload.put("dimension", instance.getCustomerCount() + 1);
        payload.put("edgeWeightType", EDGE_WEIGHT_TYPE);
        payload.put("capacity", instance.getCapacity());
        payload.put("numberOfVehicles", instance.getNumberOfVehicles());
        payload.put("depot", depot);
        payload.put("depots", List.of(depotNode));
        payload.put("customers", customers);

        return payload;
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

        @SuppressWarnings("unchecked") // Safe by convention: JSON objects use string keys and object values.
        Map<String, Object> depot = (Map<String, Object>) depotRaw;

        return new double[] {
                toDouble(depot.get("x")),
                toDouble(depot.get("y"))
        };
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