package dk.dtu.scout.backend.integrationtests.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dk.dtu.scout.backend.integrationtests.support.RunRequestFixtures.mapOf;

public final class InstanceFixtures {

    private InstanceFixtures() {
    }

    public static Map<String, Object> importPayload(String content) {
        return mapOf("content", content);
    }

    public static Map<String, Object> exportTspPayload() {
        return exportTspPayload("tiny", "", List.of(
            Map.of("x", 0.0, "y", 0.0),
            Map.of("x", 1.0, "y", 1.0)
        ));
    }

    public static Map<String, Object> exportTspPayload(
        String name,
        String comment,
        List<Map<String, Object>> cities
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("exportType", "TSP");
        payload.put("name", name);
        payload.put("comment", comment);
        payload.put("cities", cities);
        return payload;
    }

    public static Map<String, Object> exportVrpPayload() {
        return exportVrpPayload(
            "tiny",
            "",
            10,
            1,
            Map.of("x", 0.0, "y", 0.0),
            List.of(Map.of("x", 1.0, "y", 1.0, "demand", 1.0))
        );
    }

    public static Map<String, Object> exportVrpPayload(
        String name,
        String comment,
        int capacity,
        int numberOfVehicles,
        Map<String, Object> depot,
        List<Map<String, Object>> customers
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("exportType", "VRP");
        payload.put("name", name);
        payload.put("comment", comment);
        payload.put("capacity", capacity);
        payload.put("numberOfVehicles", numberOfVehicles);
        payload.put("depot", depot);
        payload.put("customers", customers);
        return payload;
    }

    public static Map<String, Object> exportVrpPayloadWithNullVehicleCount() {
        Map<String, Object> payload = exportVrpPayload();
        payload.put("numberOfVehicles", null);
        return payload;
    }

    public static Map<String, Object> tspInstancePayload() {
        return tspInstancePayload(List.of(
            Map.of("x", 0.0, "y", 0.0),
            Map.of("x", 1.0, "y", 0.0),
            Map.of("x", 1.0, "y", 1.0),
            Map.of("x", 0.0, "y", 1.0)
        ));
    }

    public static Map<String, Object> smallTspInstancePayload() {
        return tspInstancePayload(List.of(
            Map.of("x", 0.0, "y", 0.0),
            Map.of("x", 1.0, "y", 1.0)
        ));
    }

    public static Map<String, Object> vrpInstancePayload() {
        return vrpInstancePayload(List.of(
            Map.of("x", 1.0, "y", 1.0, "demand", 1.0),
            Map.of("x", 2.0, "y", 2.0, "demand", 1.0)
        ));
    }

    public static Map<String, Object> smallVrpInstancePayload() {
        return vrpInstancePayload(List.of(
            Map.of("x", 1.0, "y", 1.0, "demand", 1.0)
        ));
    }

    public static String validTsp() {
        return validTsp(true, false);
    }

    public static String validTsp(boolean withEof, boolean withBlankLines) {
        String blank = withBlankLines ? "\n" : "";

        String content = """
            NAME: tiny
            TYPE: TSP
            COMMENT: test instance
            DIMENSION: 2
            EDGE_WEIGHT_TYPE: EUC_2D
            NODE_COORD_SECTION
            1 0 0
            2 1 1
            """;

        return blank + content + (withEof ? "EOF\n" : "");
    }

    public static String validVrp() {
        return validVrp(true, false);
    }

    public static String validVrp(boolean withEof, boolean withBlankLines) {
        String blank = withBlankLines ? "\n" : "";

        String content = """
            NAME: tiny
            TYPE: CVRP
            COMMENT: test instance
            DIMENSION: 2
            EDGE_WEIGHT_TYPE: EUC_2D
            CAPACITY: 10
            NODE_COORD_SECTION
            1 0 0
            2 1 1
            DEMAND_SECTION
            1 0
            2 1
            DEPOT_SECTION
            1
            -1
            """;

        return blank + content + (withEof ? "EOF\n" : "");
    }

    public static String invalidTspMissingType() {
        return """
            NAME: invalid
            DIMENSION: 2
            EDGE_WEIGHT_TYPE: EUC_2D
            NODE_COORD_SECTION
            1 0 0
            2 1 1
            EOF
            """;
    }

    public static String unsupportedEdgeWeightType() {
        return """
            NAME: invalid
            TYPE: TSP
            DIMENSION: 2
            EDGE_WEIGHT_TYPE: MAN_2D
            NODE_COORD_SECTION
            1 0 0
            2 1 1
            EOF
            """;
    }

    private static Map<String, Object> tspInstancePayload(List<Map<String, Object>> cities) {
        Map<String, Object> payload = exportTspPayload("tiny", "", cities);
        payload.remove("exportType");
        return payload;
    }

    private static Map<String, Object> vrpInstancePayload(List<Map<String, Object>> customers) {
        Map<String, Object> payload = exportVrpPayload(
            "tiny",
            "",
            10,
            1,
            Map.of("x", 0.0, "y", 0.0),
            customers
        );
        payload.remove("exportType");
        return payload;
    }
}