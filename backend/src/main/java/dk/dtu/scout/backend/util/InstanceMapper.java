package dk.dtu.scout.backend.util;

import dk.dtu.scout.datatypes.TSPInstance;
import dk.dtu.scout.datatypes.VRPInstance;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class InstanceMapper {

    private InstanceMapper() {
    }

    public static TSPInstance toTspInstance(Object value) {
        if (value instanceof TSPInstance instance) {
            return instance;
        }
        if (!(value instanceof Map<?, ?> raw)) {
            throw new IllegalArgumentException("tspInstance must be a TSPInstance or a map");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) raw;
        String name = map.getOrDefault("name", "Custom TSP Instance").toString();
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

        return new TSPInstance(name, coordinates.length, coordinates);
    }


    public static TSPInstance parseTsplib(String content) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(content));

        String name = null;
        String type = null;
        Integer dimension = null;
        String edgeWeightType = null;
        List<double[]> coordinates = new ArrayList<>();
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("NAME:")) {
                name = line.substring(5).trim();
            } else if (line.startsWith("TYPE:")) {
                type = line.substring(5).trim();
            } else if (line.startsWith("DIMENSION:")) {
                dimension = Integer.parseInt(line.substring(10).trim());
            } else if (line.startsWith("EDGE_WEIGHT_TYPE:")) {
                edgeWeightType = line.substring(17).trim();
            } else if (line.equals("NODE_COORD_SECTION")) {
                break;
            }
        }

        if (name == null) {
            throw new IllegalArgumentException("Missing required field: NAME");
        }
        if (dimension == null) {
            throw new IllegalArgumentException("Missing required field: DIMENSION");
        }
        if (!"TSP".equals(type)) {
            throw new IllegalArgumentException("Expected TYPE: TSP, but got: " + type);
        }
        if (!"EUC_2D".equals(edgeWeightType)) {
            throw new IllegalArgumentException("Only EUC_2D edge weight type is supported, but got: " + edgeWeightType);
        }

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.equals("EOF")) break;

            String[] parts = line.split("\\s+");
            if (parts.length < 3) {
                throw new IllegalArgumentException("Invalid coordinate line: " + line);
            }

            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            coordinates.add(new double[] { x, y });
        }

        if (coordinates.size() != dimension) {
            throw new IllegalArgumentException(
                "Expected " + dimension + " coordinates, but got " + coordinates.size()
            );
        }

        double[][] coordArray = new double[coordinates.size()][2];
        for (int i = 0; i < coordinates.size(); i++) {
            coordArray[i] = coordinates.get(i);
        }

        return new TSPInstance(name, dimension, coordArray);
    }

    public static VRPInstance toVrpInstance(Object value) {
        if (value instanceof VRPInstance instance) {
            return instance;
        }
        if (!(value instanceof Map<?, ?> raw)) {
            throw new IllegalArgumentException("vrpInstance must be a VRPInstance or a map");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) raw;
        String name = map.getOrDefault("name", "Custom VRP Instance").toString();
        double capacity = toDouble(map.getOrDefault("capacity", 0));
        int numberOfVehicles = toInt(map.getOrDefault("numberOfVehicles", 1));

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
            customerDemands[i] = toDouble(customer.getOrDefault("demand", 0));
        }

        return new VRPInstance(
                name,
                depotCoordinates,
                customerCoordinates,
                customerDemands,
                capacity,
                numberOfVehicles
        );
    }

    private static double[] extractDepot(Map<String, Object> map) {
        Object depotObj = map.get("depot");
        if (depotObj instanceof Map<?, ?> depotRaw) {
            @SuppressWarnings("unchecked")
            Map<String, Object> depot = (Map<String, Object>) depotRaw;
            return new double[] { toDouble(depot.get("x")), toDouble(depot.get("y")) };
        }

        Object depotsObj = map.get("depots");
        if (depotsObj instanceof List<?> depotsList && !depotsList.isEmpty()) {
            Object first = depotsList.get(0);
            if (first instanceof Map<?, ?> depotRaw) {
                @SuppressWarnings("unchecked")
                Map<String, Object> depot = (Map<String, Object>) depotRaw;
                return new double[] { toDouble(depot.get("x")), toDouble(depot.get("y")) };
            }
        }

        throw new IllegalArgumentException("vrpInstance must include depot coordinates");
    }

    private static double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return 0;
        }
        return Double.parseDouble(value.toString());
    }

    private static int toInt(Object value) {
        if (value instanceof Number number) {
            return Math.max(1, number.intValue());
        }
        if (value == null) {
            return 1;
        }
        return Math.max(1, Integer.parseInt(value.toString()));
    }
}
