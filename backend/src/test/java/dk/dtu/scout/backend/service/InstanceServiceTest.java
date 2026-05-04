package dk.dtu.scout.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstanceServiceTest {

    private final InstanceService service = new InstanceService();

    @Test
    void importInstance_parsesTspContent() {
        Map<String, Object> result = service.importInstance(validTspContent());

        assertEquals("TSP", result.get("instanceType"));
        assertNotNull(result.get("instance"));
    }

    @Test
    void importInstance_parsesVrpContent() {
        Map<String, Object> result = service.importInstance(validVrpContent());

        assertEquals("VRP", result.get("instanceType"));
        assertNotNull(result.get("instance"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidImportContents")
    void importInstance_rejectsInvalidContent(String label, String content) {
        assertThrows(IllegalArgumentException.class, () -> service.importInstance(content));
    }

    @Test
    void exportInstance_formatsTspPayload() {
        String result = service.exportInstance(validTspPayload());

        assertTrue(result.contains("TYPE: TSP"));
        assertTrue(result.contains("NODE_COORD_SECTION"));
    }

    @Test
    void exportInstance_formatsVrpPayload() {
        String result = service.exportInstance(validVrpPayload());

        assertTrue(result.contains("TYPE: CVRP"));
        assertTrue(result.contains("DEPOT_SECTION"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidExportPayloads")
    void exportInstance_rejectsInvalidPayload(String label, Map<String, Object> payload) {
        assertThrows(IllegalArgumentException.class, () -> service.exportInstance(payload));
    }

    private static Stream<Arguments> invalidImportContents() {
        return Stream.of(
            Arguments.of("null content", null),
            Arguments.of("blank content", ""),
            Arguments.of("whitespace content", "   ")
        );
    }

    private static Stream<Arguments> invalidExportPayloads() {
        Map<String, Object> missingExportType = new HashMap<>();
        missingExportType.put("name", "tiny");

        Map<String, Object> unsupportedType = new HashMap<>();
        unsupportedType.put("exportType", "ATSP");

        return Stream.of(
            Arguments.of("null payload", null),
            Arguments.of("missing exportType", missingExportType),
            Arguments.of("unsupported exportType", unsupportedType)
        );
    }

    private static String validTspContent() {
        return String.join("\n",
            "NAME: tiny",
            "TYPE: TSP",
            "DIMENSION: 2",
            "EDGE_WEIGHT_TYPE: EUC_2D",
            "NODE_COORD_SECTION",
            "1 0 0",
            "2 3 4",
            "EOF"
        );
    }

    private static String validVrpContent() {
        return String.join("\n",
            "NAME: tiny",
            "TYPE: CVRP",
            "DIMENSION: 3",
            "EDGE_WEIGHT_TYPE: EUC_2D",
            "CAPACITY: 10",
            "NODE_COORD_SECTION",
            "1 0 0",
            "2 1 0",
            "3 0 1",
            "DEMAND_SECTION",
            "1 0",
            "2 5",
            "3 4",
            "DEPOT_SECTION",
            "1",
            "-1",
            "EOF"
        );
    }

    private static Map<String, Object> validTspPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("exportType", "TSP");
        payload.put("name", "tiny");
        payload.put("comment", "");
        payload.put("cities", List.of(
            Map.of("x", 0.0, "y", 0.0),
            Map.of("x", 1.0, "y", 1.0)
        ));
        return payload;
    }

    private static Map<String, Object> validVrpPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("exportType", "VRP");
        payload.put("name", "tiny");
        payload.put("comment", "");
        payload.put("capacity", 10);
        payload.put("numberOfVehicles", 1);
        payload.put("depot", Map.of("x", 0.0, "y", 0.0));
        payload.put("customers", List.of(Map.of("x", 1.0, "y", 1.0, "demand", 1.0)));
        return payload;
    }
}
