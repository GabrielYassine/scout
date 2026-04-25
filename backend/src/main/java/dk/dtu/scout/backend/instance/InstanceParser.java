package dk.dtu.scout.backend.instance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Parser for TSP and VRP instance files in TSPLIB format.
 * Supports TSP and CVRP/VRP types with EUC_2D edge weight type.
 * Extracts relevant data into a normalized map format used by the backend and frontend.
 * @author s235257
 */
public final class InstanceParser {

    private static final String EDGE_WEIGHT_TYPE = "EUC_2D";
    private static final Pattern HEADER_PATTERN = Pattern.compile("^([A-Z_]+)\\s*:?\\s*(.*)$", Pattern.CASE_INSENSITIVE);

    private InstanceParser() {
    }

    public static String detectInstanceType(String content) {
        Map<String, String> headers = parseHeaders(content);
        String type = headers.get("TYPE");

        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Instance file must contain a TYPE field");
        }

        return switch (type.trim().toUpperCase()) {
            case "TSP" -> "TSP";
            case "CVRP", "VRP" -> "VRP";
            default -> throw new IllegalArgumentException("Unsupported instance TYPE: " + type);
        };
    }

    /**
     * Parses TSP file content into the normalized instance map used by the backend and frontend.
     * @param content the raw content of the TSP instance file to parse
     * @return a map containing the parsed TSP instance data.
     */
    public static Map<String, Object> parseTspContent(String content) {
        Map<String, String> headers = new LinkedHashMap<>();
        List<Map<String, Object>> cities = new ArrayList<>();

        boolean inNodes = false;

        for (String rawLine : content.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;
            if (isEof(line)) break;

            if (isSection(line, "NODE_COORD_SECTION")) {
                inNodes = true;
                continue;
            }

            if (!inNodes) {
                addHeaderIfPresent(headers, line);
                continue;
            }

            String[] parts = line.split("\\s+");
            if (parts.length < 3) {
                throw new IllegalArgumentException("Invalid TSP coordinate line: " + line);
            }

            Map<String, Object> city = new LinkedHashMap<>();
            city.put("id", cities.size());
            city.put("x", toDouble(parts[1]));
            city.put("y", toDouble(parts[2]));
            cities.add(city);
        }

        requireType(headers, "TSP");
        requireSupportedEdgeWeightType(headers);

        int dimension = resolveDimension(headers, cities.size(), "NODE_COORD_SECTION contains " + cities.size() + " nodes");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", headers.getOrDefault("NAME", ""));
        result.put("comment", headers.getOrDefault("COMMENT", ""));
        result.put("type", "TSP");
        result.put("dimension", dimension);
        result.put("edgeWeightType", EDGE_WEIGHT_TYPE);
        result.put("cities", cities);

        return result;
    }

    /**
     * Parses VRP file content into the normalized instance map used by the backend and frontend.
     * @param content the raw content of the VRP instance file to parse
     * @return a map containing the parsed VRP instance data.
     */
    public static Map<String, Object> parseVrpContent(String content) {
        Map<String, String> headers = new LinkedHashMap<>();
        Map<Integer, double[]> coords = new LinkedHashMap<>();
        Map<Integer, Double> demands = new LinkedHashMap<>();
        List<Integer> depotIds = new ArrayList<>();

        String section = null;

        for (String rawLine : content.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;
            if (isEof(line)) break;

            if (isSection(line, "NODE_COORD_SECTION")) {
                section = "NODE";
                continue;
            }
            if (isSection(line, "DEMAND_SECTION")) {
                section = "DEMAND";
                continue;
            }
            if (isSection(line, "DEPOT_SECTION")) {
                section = "DEPOT";
                continue;
            }

            if (section == null) {
                addHeaderIfPresent(headers, line);
                continue;
            }

            switch (section) {
                case "NODE" -> parseCoordinateLine(line, coords);
                case "DEMAND" -> parseDemandLine(line, demands);
                case "DEPOT" -> {
                    int depotId = toInt(line);
                    if (depotId < 0) {
                        section = null;
                    } else if (depotId > 0) {
                        depotIds.add(depotId);
                    }
                }
                default -> throw new IllegalArgumentException("Unknown VRP section: " + section);
            }
        }

        requireVrpType(headers);
        requireSupportedEdgeWeightType(headers);

        if (coords.isEmpty()) {
            throw new IllegalArgumentException("VRP file must contain NODE_COORD_SECTION");
        }

        List<Integer> resolvedDepotIds = depotIds.isEmpty() ? List.of(1) : depotIds;
        if (resolvedDepotIds.size() > 1) {
            throw new IllegalArgumentException("Only single-depot CVRP instances are currently supported");
        }

        int depotNodeId = resolvedDepotIds.getFirst();
        double[] depotCoords = coords.get(depotNodeId);
        if (depotCoords == null) {
            throw new IllegalArgumentException("Depot node " + depotNodeId + " is missing coordinates");
        }

        double capacity = requireCapacity(headers);
        int numberOfVehicles = requireVehicleCount(headers);
        validateDemandsExist(coords, demands, resolvedDepotIds);

        List<Map<String, Object>> customers = buildCustomers(coords, demands, resolvedDepotIds);

        int expectedNodeCount = customers.size() + 1;
        int dimension = resolveDimension(headers, expectedNodeCount, "instance contains " + expectedNodeCount + " nodes");

        String name = headers.getOrDefault("NAME", "");

        Map<String, Object> depot = new LinkedHashMap<>();
        depot.put("x", depotCoords[0]);
        depot.put("y", depotCoords[1]);

        Map<String, Object> depotNode = new LinkedHashMap<>();
        depotNode.put("id", 0);
        depotNode.put("nodeId", depotNodeId);
        depotNode.put("x", depotCoords[0]);
        depotNode.put("y", depotCoords[1]);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", name);
        result.put("comment", headers.getOrDefault("COMMENT", ""));
        result.put("type", "CVRP");
        result.put("dimension", dimension);
        result.put("edgeWeightType", EDGE_WEIGHT_TYPE);
        result.put("capacity", capacity);
        result.put("numberOfVehicles", numberOfVehicles);
        result.put("depot", depot);
        result.put("depots", List.of(depotNode));
        result.put("customers", customers);

        return result;
    }

    private static void parseCoordinateLine(String line, Map<Integer, double[]> coords) {
        String[] parts = line.split("\\s+");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid coordinate line: " + line);
        }

        int nodeId = toInt(parts[0]);
        double x = toDouble(parts[1]);
        double y = toDouble(parts[2]);

        coords.put(nodeId, new double[] { x, y });
    }

    private static void parseDemandLine(String line, Map<Integer, Double> demands) {
        String[] parts = line.split("\\s+");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid demand line: " + line);
        }

        int nodeId = toInt(parts[0]);
        double demand = toDouble(parts[1]);

        demands.put(nodeId, demand);
    }

    private static List<Map<String, Object>> buildCustomers(
            Map<Integer, double[]> coords,
            Map<Integer, Double> demands,
            List<Integer> depotIds
    ) {
        List<Map<String, Object>> customers = new ArrayList<>();

        for (Map.Entry<Integer, double[]> entry : coords.entrySet()) {
            int nodeId = entry.getKey();
            if (depotIds.contains(nodeId)) continue;

            double[] point = entry.getValue();

            Map<String, Object> customer = new LinkedHashMap<>();
            customer.put("id", customers.size());
            customer.put("x", point[0]);
            customer.put("y", point[1]);
            customer.put("demand", demands.get(nodeId));
            customer.put("originalId", nodeId);
            customers.add(customer);
        }

        return customers;
    }

    private static Map<String, String> parseHeaders(String content) {
        Map<String, String> headers = new LinkedHashMap<>();

        for (String rawLine : content.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            if (isEof(line) || isKnownSection(line)) {
                break;
            }

            addHeaderIfPresent(headers, line);
        }

        return headers;
    }

    private static void addHeaderIfPresent(Map<String, String> headers, String line) {
        HeaderLine parsed = parseHeaderLine(line);
        if (parsed != null) {
            headers.put(parsed.key(), parsed.value());
        }
    }

    private static void requireType(Map<String, String> headers, String expectedType) {
        String type = headers.get("TYPE");
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Instance file must contain a TYPE field");
        }

        if (!expectedType.equalsIgnoreCase(type)) {
            throw new IllegalArgumentException("Expected TYPE: " + expectedType + ", but got: " + type);
        }
    }

    private static void requireVrpType(Map<String, String> headers) {
        String type = headers.get("TYPE");
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Instance file must contain a TYPE field");
        }

        if (!"CVRP".equalsIgnoreCase(type) && !"VRP".equalsIgnoreCase(type)) {
            throw new IllegalArgumentException("Expected TYPE: CVRP or VRP, but got: " + type);
        }
    }

    private static void requireSupportedEdgeWeightType(Map<String, String> headers) {
        String edgeWeightType = headers.getOrDefault("EDGE_WEIGHT_TYPE", EDGE_WEIGHT_TYPE);

        if (!EDGE_WEIGHT_TYPE.equalsIgnoreCase(edgeWeightType)) {
            throw new IllegalArgumentException("Only EUC_2D edge weight type is supported, but got: " + edgeWeightType);
        }
    }

    private static double requireCapacity(Map<String, String> headers) {
        if (!headers.containsKey("CAPACITY")) {
            throw new IllegalArgumentException("VRP file must contain a CAPACITY field");
        }

        return toDouble(headers.get("CAPACITY"));
    }

    private static int requireVehicleCount(Map<String, String> headers) {
        if (!headers.containsKey("VEHICLES")) {
            throw new IllegalArgumentException("VRP file must contain a VEHICLES field");
        }

        int vehicles = toInt(headers.get("VEHICLES"));
        if (vehicles <= 0) {
            throw new IllegalArgumentException("VEHICLES must be positive");
        }

        return vehicles;
    }

    private static void validateDemandsExist(
            Map<Integer, double[]> coords,
            Map<Integer, Double> demands,
            List<Integer> depotIds
    ) {
        if (demands.isEmpty()) {
            throw new IllegalArgumentException("VRP file must contain DEMAND_SECTION");
        }

        for (Integer nodeId : coords.keySet()) {
            if (depotIds.contains(nodeId)) {
                continue;
            }

            if (!demands.containsKey(nodeId)) {
                throw new IllegalArgumentException("Missing demand for customer node " + nodeId);
            }
        }
    }

    private static int resolveDimension(Map<String, String> headers, int actualDimension, String actualDescription) {
        int dimension = headers.containsKey("DIMENSION")
                ? toInt(headers.get("DIMENSION"))
                : actualDimension;

        if (dimension != actualDimension) {
            throw new IllegalArgumentException(
                    "DIMENSION is " + dimension + ", but " + actualDescription
            );
        }

        return dimension;
    }

    private static HeaderLine parseHeaderLine(String line) {
        var matcher = HEADER_PATTERN.matcher(line);

        if (!matcher.matches()) {
            return null;
        }

        return new HeaderLine(
                matcher.group(1).toUpperCase(),
                matcher.group(2) == null ? "" : matcher.group(2).trim()
        );
    }

    private static boolean isKnownSection(String line) {
        return isSection(line, "NODE_COORD_SECTION")
                || isSection(line, "DEMAND_SECTION")
                || isSection(line, "DEPOT_SECTION");
    }

    private static boolean isSection(String line, String sectionName) {
        return sectionName.equalsIgnoreCase(line);
    }

    private static boolean isEof(String line) {
        return "EOF".equalsIgnoreCase(line);
    }

    private static double toDouble(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        if (value == null) throw new IllegalArgumentException("Missing numeric value");
        return Double.parseDouble(value.toString());
    }

    private static int toInt(Object value) {
        if (value instanceof Number number) return number.intValue();
        if (value == null) throw new IllegalArgumentException("Missing integer value");
        return Integer.parseInt(value.toString());
    }

    private record HeaderLine(String key, String value) {
    }
}