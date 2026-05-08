package dk.dtu.scout.backend.instance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Parser for TSP and CVRP instance files in a supported TSPLIB95 subset.
 * Supports TYPE TSP and CVRP with EDGE_WEIGHT_TYPE EUC_2D and NODE_COORD_SECTION.
 * CVRP additionally requires CAPACITY, DEMAND_SECTION, and DEPOT_SECTION.
 * EOF is accepted but optional, as defined by TSPLIB95.
 * @author s235257
 */
public final class InstanceParser {

    private static final String EDGE_WEIGHT_TYPE = "EUC_2D";
    private static final Pattern HEADER_PATTERN = Pattern.compile("^([A-Z_]+)\\s*:?\\s*(.*)$", Pattern.CASE_INSENSITIVE);

    private enum VrpSection {
        NODE,
        DEMAND,
        DEPOT
    }

    private InstanceParser() {
    }
    /**
     * Detects the instance type (TSP or CVRP) from the raw content of the instance file.
     * @param content the raw content of the instance file to analyze
     * @return the detected instance type, either "TSP" or "CVRP"
     * @throws IllegalArgumentException if the TYPE field is missing, empty, or contains an unsupported value
     */
    public static String detectInstanceType(String content) {
        Map<String, String> headers = parseHeaders(content);
        String type = headers.get("TYPE");

        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Instance file must contain a TYPE field");
        }

        return switch (type.trim().toUpperCase()) {
            case "TSP" -> "TSP";
            case "CVRP" -> "VRP";
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

        boolean seenNodeSection = false;
        boolean inNodes = false;

        for (String rawLine : content.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;
            if (isEof(line)) break;

            if (isSection(line, "NODE_COORD_SECTION")) {
                seenNodeSection = true;
                inNodes = true;
                continue;
            }

            if (!inNodes) {
                addHeader(headers, line);
                continue;
            }

            String[] parts = line.split("\\s+");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid TSP coordinate line: " + line);
            }

            int nodeId = toInt(parts[0]);

            Map<String, Object> city = new LinkedHashMap<>();
            city.put("id", nodeId);
            city.put("nodeId", nodeId);
            city.put("x", toDouble(parts[1]));
            city.put("y", toDouble(parts[2]));
            cities.add(city);
        }

        requireSupportedEdgeWeightType(headers);

        if (!seenNodeSection || cities.isEmpty()) {
            throw new IllegalArgumentException("TSP file must contain NODE_COORD_SECTION");
        }

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

        boolean seenNodeSection = false;
        boolean seenDemandSection = false;
        boolean seenDepotSection = false;
        boolean depotSectionTerminated = false;

        VrpSection section = null;

        for (String rawLine : content.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;
            if (isEof(line)) break;

            if (isSection(line, "NODE_COORD_SECTION")) {
                seenNodeSection = true;
                section = VrpSection.NODE;
                continue;
            }

            if (isSection(line, "DEMAND_SECTION")) {
                seenDemandSection = true;
                section = VrpSection.DEMAND;
                continue;
            }

            if (isSection(line, "DEPOT_SECTION")) {
                seenDepotSection = true;
                section = VrpSection.DEPOT;
                continue;
            }

            if (section == null) {
                addHeader(headers, line);
                continue;
            }

            if (section == VrpSection.NODE) {
                parseCoordinateLine(line, coords);
            } else if (section == VrpSection.DEMAND) {
                parseDemandLine(line, demands);
            } else {
                int depotId = toInt(line);
                if (depotId < 0) {
                    depotSectionTerminated = true;
                    section = null;
                } else {
                    depotIds.add(depotId);
                }
            }
        }

        requireSupportedEdgeWeightType(headers);

        if (!seenNodeSection || coords.isEmpty()) {
            throw new IllegalArgumentException("VRP file must contain NODE_COORD_SECTION");
        }

        if (!seenDemandSection || demands.isEmpty()) {
            throw new IllegalArgumentException("VRP file must contain DEMAND_SECTION");
        }

        if (!seenDepotSection || depotIds.isEmpty()) {
            throw new IllegalArgumentException("VRP file must contain DEPOT_SECTION");
        }

        if (!depotSectionTerminated) {
            throw new IllegalArgumentException("DEPOT_SECTION must be terminated by -1");
        }

        if (depotIds.size() > 1) {
            throw new IllegalArgumentException("Only single-depot CVRP instances are currently supported");
        }

        int depotNodeId = depotIds.getFirst();
        double[] depotCoords = coords.get(depotNodeId);

        if (depotCoords == null) {
            throw new IllegalArgumentException("Depot node " + depotNodeId + " is missing coordinates");
        }

        double capacity = requireCapacity(headers);
        int numberOfVehicles = resolveVehicleCount(headers);

        validateDemandsExist(coords, demands, depotIds);

        List<Map<String, Object>> customers = buildCustomers(coords, demands, depotIds);

        int expectedNodeCount = customers.size() + 1;
        int dimension = resolveDimension(headers, expectedNodeCount, "instance contains " + expectedNodeCount + " nodes");

        Map<String, Object> depot = buildNode(depotNodeId, depotCoords);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", headers.getOrDefault("NAME", ""));
        result.put("comment", headers.getOrDefault("COMMENT", ""));
        result.put("type", "CVRP");
        result.put("dimension", dimension);
        result.put("edgeWeightType", EDGE_WEIGHT_TYPE);
        result.put("capacity", capacity);
        result.put("numberOfVehicles", numberOfVehicles);
        result.put("depot", depot);
        result.put("depots", List.of(depot));
        result.put("customers", customers);

        return result;
    }

    private static Map<String, Object> buildNode(int nodeId, double[] coords) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", nodeId);
        node.put("nodeId", nodeId);
        node.put("x", coords[0]);
        node.put("y", coords[1]);
        return node;
    }

    private static void parseCoordinateLine(String line, Map<Integer, double[]> coords) {
        String[] parts = line.split("\\s+");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid coordinate line: " + line);
        }

        int nodeId = toInt(parts[0]);
        double x = toDouble(parts[1]);
        double y = toDouble(parts[2]);

        coords.put(nodeId, new double[] { x, y });
    }

    private static void parseDemandLine(String line, Map<Integer, Double> demands) {
        String[] parts = line.split("\\s+");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid demand line: " + line);
        }

        int nodeId = toInt(parts[0]);
        double demand = toDouble(parts[1]);

        if (demand < 0) {
            throw new IllegalArgumentException("Demand for node " + nodeId + " cannot be negative");
        }

        demands.put(nodeId, demand);
    }

    private static List<Map<String, Object>> buildCustomers(Map<Integer, double[]> coords, Map<Integer, Double> demands, List<Integer> depotIds) {
        List<Map<String, Object>> customers = new ArrayList<>();

        for (Map.Entry<Integer, double[]> entry : coords.entrySet()) {
            int nodeId = entry.getKey();
            if (depotIds.contains(nodeId)) continue;

            Map<String, Object> customer = buildNode(nodeId, entry.getValue());
            customer.put("demand", demands.get(nodeId));
            customers.add(customer);
        }

        return customers;
    }

    private static Map<String, String> parseHeaders(String content) {
        Map<String, String> headers = new LinkedHashMap<>();

        for (String rawLine : content.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;
            if (isEof(line)) break;

            if (isKnownSection(line)) {
                break;
            }

            addHeader(headers, line);
        }

        return headers;
    }

    private static void addHeader(Map<String, String> headers, String line) {
        HeaderLine parsed = parseHeaderLine(line);
        headers.put(parsed.key(), parsed.value());
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

        double capacity = toDouble(headers.get("CAPACITY"));

        if (capacity < 0) {
            throw new IllegalArgumentException("VRP capacity cannot be negative");
        }

        return capacity;
    }

    private static int resolveVehicleCount(Map<String, String> headers) {
        if (!headers.containsKey("VEHICLES")) {
            return 1;
        }

        int vehicles = toInt(headers.get("VEHICLES"));
        if (vehicles <= 0) {
            throw new IllegalArgumentException("VEHICLES must be positive");
        }

        return vehicles;
    }

    private static void validateDemandsExist(Map<Integer, double[]> coords, Map<Integer, Double> demands, List<Integer> depotIds) {
        for (Integer depotId : depotIds) {
            if (!demands.containsKey(depotId)) {
                throw new IllegalArgumentException("Missing demand for depot node " + depotId);
            }

            if (demands.get(depotId) != 0.0) {
                throw new IllegalArgumentException("Depot node " + depotId + " demand must be 0");
            }
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
        int dimension = headers.containsKey("DIMENSION") ? toInt(headers.get("DIMENSION")) : actualDimension;

        if (dimension != actualDimension) {
            throw new IllegalArgumentException("DIMENSION is " + dimension + ", but " + actualDescription);
        }

        return dimension;
    }

    private static HeaderLine parseHeaderLine(String line) {
        var matcher = HEADER_PATTERN.matcher(line);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid header line: " + line);
        }

        return new HeaderLine(matcher.group(1).toUpperCase(), matcher.group(2).trim());
    }

    private static boolean isKnownSection(String line) {
        return isSection(line, "NODE_COORD_SECTION")
                || isSection(line, "DEMAND_SECTION")
                || isSection(line, "DEPOT_SECTION");
    }

    private static boolean isSection(String line, String sectionName) {
        String normalized = line.trim();

        if (normalized.endsWith(":")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }

        return sectionName.equalsIgnoreCase(normalized);
    }

    private static boolean isEof(String line) {
        return "EOF".equalsIgnoreCase(line.trim());
    }

    private static double toDouble(String value) {
        return Double.parseDouble(value);
    }

    private static int toInt(String value) {
        return Integer.parseInt(value);
    }

    private record HeaderLine(String key, String value) {
    }
}