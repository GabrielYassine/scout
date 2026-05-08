package dk.dtu.scout.backend.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dtu.scout.backend.integrationtests.support.BackendJsonTestSupport;
import dk.dtu.scout.backend.integrationtests.support.InstanceFixtures;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InstanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    class ImportEndpoint {

        @Test
        void importTsp_withEof_returnsParsedInstance() throws Exception {
            postImport(InstanceFixtures.importPayload(InstanceFixtures.validTsp(true, false)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.instanceType").value("TSP"))
                    .andExpect(jsonPath("$.instance.type").value("TSP"))
                    .andExpect(jsonPath("$.instance.name").value("tiny"))
                    .andExpect(jsonPath("$.instance.dimension").value(2))
                    .andExpect(jsonPath("$.instance.edgeWeightType").value("EUC_2D"))
                    .andExpect(jsonPath("$.instance.cities", hasSize(2)))
                    .andExpect(jsonPath("$.instance.cities[0].nodeId").value(1))
                    .andExpect(jsonPath("$.instance.cities[0].x").value(0.0))
                    .andExpect(jsonPath("$.instance.cities[1].nodeId").value(2))
                    .andExpect(jsonPath("$.instance.cities[1].y").value(1.0));
        }

        @Test
        void importTsp_withoutEofAndWithBlankLines_returnsParsedInstance() throws Exception {
            postImport(InstanceFixtures.importPayload(InstanceFixtures.validTsp(false, true)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.instanceType").value("TSP"))
                    .andExpect(jsonPath("$.instance.dimension").value(2))
                    .andExpect(jsonPath("$.instance.cities", hasSize(2)));
        }

        @Test
        void importVrp_withEof_returnsParsedInstance() throws Exception {
            postImport(InstanceFixtures.importPayload(InstanceFixtures.validVrp(true, false)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.instanceType").value("VRP"))
                    .andExpect(jsonPath("$.instance.type").value("CVRP"))
                    .andExpect(jsonPath("$.instance.name").value("tiny"))
                    .andExpect(jsonPath("$.instance.dimension").value(2))
                    .andExpect(jsonPath("$.instance.edgeWeightType").value("EUC_2D"))
                    .andExpect(jsonPath("$.instance.capacity").value(10.0))
                    .andExpect(jsonPath("$.instance.numberOfVehicles").value(1))
                    .andExpect(jsonPath("$.instance.depot.nodeId").value(1))
                    .andExpect(jsonPath("$.instance.customers", hasSize(1)))
                    .andExpect(jsonPath("$.instance.customers[0].nodeId").value(2))
                    .andExpect(jsonPath("$.instance.customers[0].demand").value(1.0));
        }

        @Test
        void importVrp_withoutEofAndWithBlankLines_returnsParsedInstance() throws Exception {
            postImport(InstanceFixtures.importPayload(InstanceFixtures.validVrp(false, true)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.instanceType").value("VRP"))
                    .andExpect(jsonPath("$.instance.dimension").value(2))
                    .andExpect(jsonPath("$.instance.customers", hasSize(1)));
        }

        @Test
        void importVrp_withVehiclesAndZeroDepotId_returnsParsedInstance() throws Exception {
            postImport(InstanceFixtures.importPayload(validVrpWithVehiclesAndZeroDepotId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.instanceType").value("VRP"))
                    .andExpect(jsonPath("$.instance.numberOfVehicles").value(2))
                    .andExpect(jsonPath("$.instance.depot.nodeId").value(1))
                    .andExpect(jsonPath("$.instance.customers", hasSize(1)));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("dk.dtu.scout.backend.integrationtests.InstanceIntegrationTest#invalidImportPayloads")
        void importInstance_rejectsInvalidPayloads(String label, Object payload, String expectedMessage) throws Exception {
            assertBadRequest(postImport(payload), expectedMessage);
        }
    }

    @Nested
    class ExportEndpoint {

        @Test
        void exportTsp_returnsPlainTextTspFile() throws Exception {
            assertOkTextExport(postExport(InstanceFixtures.exportTspPayload()))
                    .andExpect(content().string(containsString("NAME: tiny")))
                    .andExpect(content().string(containsString("TYPE: TSP")))
                    .andExpect(content().string(containsString("DIMENSION: 2")))
                    .andExpect(content().string(containsString("EDGE_WEIGHT_TYPE: EUC_2D")))
                    .andExpect(content().string(containsString("NODE_COORD_SECTION")))
                    .andExpect(content().string(containsString("1 0 0")))
                    .andExpect(content().string(containsString("2 1 1")))
                    .andExpect(content().string(containsString("EOF")));
        }

        @Test
        void exportTsp_withOmittedNameAndBlankComment_usesFallbackAndSkipsComment() throws Exception {
            assertOkTextExport(postExport(tspExportWithBlankNameAndStringCoordinates()))
                    .andExpect(content().string(containsString("NAME: Custom TSP Instance")))
                    .andExpect(content().string(not(containsString("COMMENT:"))))
                    .andExpect(content().string(containsString("1 1.5 2.5")))
                    .andExpect(content().string(containsString("2 3 4")));
        }

        @Test
        void exportVrp_returnsPlainTextVrpFile() throws Exception {
            assertOkTextExport(postExport(InstanceFixtures.exportVrpPayload()))
                    .andExpect(content().string(containsString("NAME: tiny")))
                    .andExpect(content().string(containsString("TYPE: CVRP")))
                    .andExpect(content().string(containsString("DIMENSION: 2")))
                    .andExpect(content().string(containsString("EDGE_WEIGHT_TYPE: EUC_2D")))
                    .andExpect(content().string(containsString("CAPACITY: 10")))
                    .andExpect(content().string(containsString("NODE_COORD_SECTION")))
                    .andExpect(content().string(containsString("DEMAND_SECTION")))
                    .andExpect(content().string(containsString("DEPOT_SECTION")))
                    .andExpect(content().string(containsString("-1")))
                    .andExpect(content().string(containsString("EOF")));
        }

        @Test
        void exportVrp_withOmittedNameNumericStringsAndMessyComment_formatsCleanly() throws Exception {
            assertOkTextExport(postExport(vrpExportWithBlankNameAndStringNumbers()))
                    .andExpect(content().string(containsString("NAME: Custom VRP Instance")))
                    .andExpect(content().string(containsString("COMMENT: sample comment")))
                    .andExpect(content().string(containsString("CAPACITY: 10")))
                    .andExpect(content().string(containsString("1 0 0")))
                    .andExpect(content().string(containsString("2 1.25 2.5")))
                    .andExpect(content().string(containsString("2 3")));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("dk.dtu.scout.backend.integrationtests.InstanceIntegrationTest#invalidExportPayloads")
        void exportInstance_rejectsInvalidPayloads(String label, Object payload, String expectedMessage) throws Exception {
            assertBadRequest(postExport(payload), expectedMessage);
        }
    }

    private ResultActions postImport(Object payload) throws Exception {
        return BackendJsonTestSupport.postJson(mockMvc, objectMapper, "/api/instance/import", payload);
    }

    private ResultActions postExport(Object payload) throws Exception {
        return BackendJsonTestSupport.postJson(mockMvc, objectMapper, "/api/instance/export", payload);
    }

    private static ResultActions assertOkTextExport(ResultActions result) throws Exception {
        return result
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain; charset=utf-8"));
    }

    private static void assertBadRequest(ResultActions result, String expectedMessage) throws Exception {
        result
                .andExpect(status().isBadRequest())
                .andExpect(content().string(expectedMessage));
    }

    private static Map<String, Object> tspExportWithBlankNameAndStringCoordinates() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("exportType", "TSP");
        payload.put("name", "   ");
        payload.put("comment", "   ");
        payload.put("cities", List.of(
                Map.of("x", "1.5", "y", "2.5"),
                Map.of("x", "3", "y", "4")
        ));
        return payload;
    }

    private static Map<String, Object> vrpExportWithBlankNameAndStringNumbers() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("exportType", "VRP");
        payload.put("name", "   ");
        payload.put("comment", "  sample   comment ");
        payload.put("capacity", "10");
        payload.put("numberOfVehicles", "2");
        payload.put("depot", Map.of("x", "0", "y", "0"));
        payload.put("customers", List.of(
                Map.of("x", "1.25", "y", "2.5", "demand", "3")
        ));
        return payload;
    }

    private static Stream<Arguments> invalidImportPayloads() {
        return Stream.of(
                invalidImport("invalid header line", "Invalid header line: 12345",
                        "NAME: tiny",
                        "12345",
                        "TYPE: TSP",
                        "EDGE_WEIGHT_TYPE: EUC_2D",
                        "NODE_COORD_SECTION",
                        "1 0 0",
                        "2 1 1",
                        "EOF"
                ),
                Arguments.of("missing content field", Map.of(), "Instance file content must be provided"),
                Arguments.of("blank content", InstanceFixtures.importPayload(""), "Instance file content must be provided"),
                Arguments.of("whitespace content", InstanceFixtures.importPayload("   "), "Instance file content must be provided"),
                Arguments.of("missing TYPE", InstanceFixtures.importPayload("NAME: tiny\n"), "Instance file must contain a TYPE field"),
                Arguments.of("blank TYPE", InstanceFixtures.importPayload("TYPE:   \n"), "Instance file must contain a TYPE field"),
                Arguments.of("unsupported TYPE", InstanceFixtures.importPayload("TYPE: ATSP\n"), "Unsupported instance TYPE: ATSP"),

                invalidImport("TSP invalid coordinate line", "Invalid TSP coordinate line: 1 2",
                        "NAME: tiny",
                        "TYPE: TSP",
                        "NODE_COORD_SECTION",
                        "1 2",
                        "EOF"
                ),
                invalidImport("TSP unsupported edge weight type", "Only EUC_2D edge weight type is supported, but got: GEO",
                        "NAME: tiny",
                        "TYPE: TSP",
                        "EDGE_WEIGHT_TYPE: GEO",
                        "NODE_COORD_SECTION",
                        "1 0 0",
                        "EOF"
                ),
                invalidImport("TSP dimension mismatch", "DIMENSION is 3, but NODE_COORD_SECTION contains 2 nodes",
                        "NAME: tiny",
                        "TYPE: TSP",
                        "DIMENSION: 3",
                        "NODE_COORD_SECTION",
                        "1 0 0",
                        "2 1 1",
                        "EOF"
                ),
                invalidImport("VRP invalid coordinate line", "Invalid coordinate line: 1 0",
                        "NAME: tiny",
                        "TYPE: CVRP",
                        "CAPACITY: 10",
                        "NODE_COORD_SECTION",
                        "1 0",
                        "EOF"
                ),
                invalidImport("VRP invalid demand line", "Invalid demand line: 1",
                        "NAME: tiny",
                        "TYPE: CVRP",
                        "CAPACITY: 10",
                        "NODE_COORD_SECTION",
                        "1 0 0",
                        "DEMAND_SECTION",
                        "1",
                        "EOF"
                ),
                invalidImport("VRP unsupported edge weight type", "Only EUC_2D edge weight type is supported, but got: GEO",
                        "NAME: tiny",
                        "TYPE: CVRP",
                        "EDGE_WEIGHT_TYPE: GEO",
                        "CAPACITY: 10",
                        "NODE_COORD_SECTION",
                        "1 0 0",
                        "DEMAND_SECTION",
                        "1 0",
                        "DEPOT_SECTION",
                        "1",
                        "-1",
                        "EOF"
                ),
                invalidImport("VRP missing capacity", "VRP file must contain a CAPACITY field",
                        "NAME: tiny",
                        "TYPE: CVRP",
                        "NODE_COORD_SECTION",
                        "1 0 0",
                        "DEMAND_SECTION",
                        "1 0",
                        "DEPOT_SECTION",
                        "1",
                        "-1",
                        "EOF"
                ),
                invalidImport("VRP missing demand section", "VRP file must contain DEMAND_SECTION",
                        "NAME: tiny",
                        "TYPE: CVRP",
                        "CAPACITY: 10",
                        "NODE_COORD_SECTION",
                        "1 0 0",
                        "DEPOT_SECTION",
                        "1",
                        "-1",
                        "EOF"
                ),
                invalidImport("VRP missing depot section", "VRP file must contain DEPOT_SECTION",
                        "NAME: tiny",
                        "TYPE: CVRP",
                        "CAPACITY: 10",
                        "NODE_COORD_SECTION",
                        "1 0 0",
                        "DEMAND_SECTION",
                        "1 0",
                        "EOF"
                ),
                invalidImport("VRP multiple depots", "Only single-depot CVRP instances are currently supported",
                        "NAME: tiny",
                        "TYPE: CVRP",
                        "CAPACITY: 10",
                        "NODE_COORD_SECTION",
                        "1 0 0",
                        "2 1 1",
                        "DEMAND_SECTION",
                        "1 0",
                        "2 1",
                        "DEPOT_SECTION",
                        "1",
                        "2",
                        "-1",
                        "EOF"
                ),
                invalidImport("VRP missing node section", "VRP file must contain NODE_COORD_SECTION",
                        "NAME: tiny",
                        "TYPE: CVRP",
                        "CAPACITY: 10",
                        "DEMAND_SECTION",
                        "1 0",
                        "DEPOT_SECTION",
                        "1",
                        "-1",
                        "EOF"
                ),
                invalidImport("VRP depot missing coordinates", "Depot node 2 is missing coordinates",
                        "NAME: tiny",
                        "TYPE: CVRP",
                        "CAPACITY: 10",
                        "NODE_COORD_SECTION",
                        "1 0 0",
                        "DEMAND_SECTION",
                        "1 0",
                        "DEPOT_SECTION",
                        "2",
                        "-1",
                        "EOF"
                ),
                invalidImport("VRP missing customer demand", "Missing demand for customer node 2",
                        "NAME: tiny",
                        "TYPE: CVRP",
                        "CAPACITY: 10",
                        "NODE_COORD_SECTION",
                        "1 0 0",
                        "2 1 1",
                        "DEMAND_SECTION",
                        "1 0",
                        "DEPOT_SECTION",
                        "1",
                        "-1",
                        "EOF"
                ),
                invalidImport("VRP invalid vehicle count", "VEHICLES must be positive",
                        "NAME: tiny",
                        "TYPE: CVRP",
                        "CAPACITY: 10",
                        "VEHICLES: 0",
                        "NODE_COORD_SECTION",
                        "1 0 0",
                        "DEMAND_SECTION",
                        "1 0",
                        "DEPOT_SECTION",
                        "1",
                        "-1",
                        "EOF"
                )
        );
    }

    private static Stream<Arguments> invalidExportPayloads() {
        return Stream.of(
                Arguments.of("missing exportType", Map.of("name", "tiny"), "exportType must be provided"),
                Arguments.of("blank exportType", Map.of("exportType", "   "), "exportType must be provided"),
                Arguments.of("unsupported exportType", Map.of("exportType", "ATSP"), "Unsupported exportType: ATSP"),
                Arguments.of("TSP missing cities", Map.of("exportType", "TSP", "name", "tiny"), "tspInstance must contain a non-empty cities list"),
                Arguments.of("TSP empty cities", Map.of("exportType", "TSP", "cities", List.of()), "tspInstance must contain a non-empty cities list"),
                Arguments.of("TSP city not object", Map.of("exportType", "TSP", "cities", List.of("not-a-map")), "tspInstance city must be a map"),
                Arguments.of("TSP missing coordinate", Map.of("exportType", "TSP", "cities", List.of(Map.of("x", 1.0))), "Missing numeric value"),

                Arguments.of("VRP missing depot", vrpPayloadWithout("depot"), "vrpInstance must include depot coordinates"),
                Arguments.of("VRP depot not object", vrpPayload("depot", "not-a-map"), "vrpInstance must include depot coordinates"),
                Arguments.of("VRP missing capacity", vrpPayloadWithout("capacity"), "Missing numeric value"),
                Arguments.of("VRP customers not list", vrpPayload("customers", "not-a-list"), "vrpInstance must contain a non-empty customers list"),
                Arguments.of("VRP empty customers", vrpPayload("customers", List.of()), "vrpInstance must contain a non-empty customers list"),
                Arguments.of("VRP customer not object", vrpPayload("customers", List.of("not-a-map")), "vrpInstance customer must be a map"),
                Arguments.of("VRP missing customer demand", vrpPayload("customers", List.of(Map.of("x", 1.0, "y", 1.0))), "Missing numeric value"),
                Arguments.of("VRP negative capacity", vrpPayload("capacity", -1), "VRP capacity cannot be negative"),
                Arguments.of("VRP invalid vehicle count", vrpPayload("numberOfVehicles", 0), "Number of vehicles must be positive"),
                Arguments.of("VRP null vehicle count", InstanceFixtures.exportVrpPayloadWithNullVehicleCount(), "Missing integer value"),
                Arguments.of("VRP negative demand", vrpPayload("customers", List.of(Map.of("x", 1.0, "y", 1.0, "demand", -1.0))), "VRP customer 1 demand cannot be negative"),
                Arguments.of("VRP demand exceeds capacity", vrpPayload("customers", List.of(Map.of("x", 1.0, "y", 1.0, "demand", 11.0))), "VRP customer 1 demand exceeds vehicle capacity")
        );
    }

    private static Arguments invalidImport(String label, String expectedMessage, String... lines) {
        return Arguments.of(
                label,
                InstanceFixtures.importPayload(String.join("\n", lines)),
                expectedMessage
        );
    }

    private static Map<String, Object> vrpPayload(String key, Object value) {
        Map<String, Object> payload = new LinkedHashMap<>(InstanceFixtures.exportVrpPayload());
        payload.put(key, value);
        return payload;
    }

    private static Map<String, Object> vrpPayloadWithout(String key) {
        Map<String, Object> payload = new LinkedHashMap<>(InstanceFixtures.exportVrpPayload());
        payload.remove(key);
        return payload;
    }

    private static String validVrpWithVehiclesAndZeroDepotId() {
        return String.join("\n",
                "NAME: tiny",
                "TYPE: CVRP",
                "EDGE_WEIGHT_TYPE: EUC_2D",
                "CAPACITY: 10",
                "VEHICLES: 2",
                "NODE_COORD_SECTION",
                "1 0 0",
                "2 1 1",
                "DEMAND_SECTION",
                "1 0",
                "2 1",
                "DEPOT_SECTION",
                "0",
                "1",
                "-1",
                "EOF"
        );
    }
}