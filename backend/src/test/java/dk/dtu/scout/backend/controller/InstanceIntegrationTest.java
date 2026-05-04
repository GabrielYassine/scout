package dk.dtu.scout.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
            postImport(Map.of("content", validTsp(true, false)))
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
            postImport(Map.of("content", validTsp(false, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instanceType").value("TSP"))
                .andExpect(jsonPath("$.instance.dimension").value(2))
                .andExpect(jsonPath("$.instance.cities", hasSize(2)));
        }

        @Test
        void importVrp_withEof_returnsParsedInstance() throws Exception {
            postImport(Map.of("content", validVrp(true, false)))
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
            postImport(Map.of("content", validVrp(false, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instanceType").value("VRP"))
                .andExpect(jsonPath("$.instance.dimension").value(2))
                .andExpect(jsonPath("$.instance.customers", hasSize(1)));
        }

        @Test
        void importVrp_withVehiclesAndZeroDepotId_returnsParsedInstance() throws Exception {
            postImport(Map.of("content", validVrpWithVehiclesAndZeroDepotId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instanceType").value("VRP"))
                .andExpect(jsonPath("$.instance.numberOfVehicles").value(2))
                .andExpect(jsonPath("$.instance.depot.nodeId").value(1))
                .andExpect(jsonPath("$.instance.customers", hasSize(1)));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("dk.dtu.scout.backend.controller.InstanceIntegrationTest#invalidImportPayloads")
        void importInstance_rejectsInvalidPayloads(String label, Object payload, String expectedMessage) throws Exception {
            postImport(payload)
                .andExpect(status().isBadRequest())
                .andExpect(content().string(expectedMessage));
        }
    }

    @Nested
    class ExportEndpoint {

        @Test
        void exportTsp_returnsPlainTextTspFile() throws Exception {
            postExport(validTspPayload())
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain; charset=utf-8"))
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
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("exportType", "TSP");
            payload.put("name", "   ");
            payload.put("comment", "   ");
            payload.put("cities", List.of(
                Map.of("x", "1.5", "y", "2.5"),
                Map.of("x", "3", "y", "4")
            ));

            postExport(payload)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("NAME: Custom TSP Instance")))
                .andExpect(content().string(not(containsString("COMMENT:"))))
                .andExpect(content().string(containsString("1 1.5 2.5")))
                .andExpect(content().string(containsString("2 3 4")));
        }

        @Test
        void exportVrp_returnsPlainTextVrpFile() throws Exception {
            postExport(validVrpPayload())
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain; charset=utf-8"))
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

            postExport(payload)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("NAME: Custom VRP Instance")))
                .andExpect(content().string(containsString("COMMENT: sample comment")))
                .andExpect(content().string(containsString("CAPACITY: 10")))
                .andExpect(content().string(containsString("1 0 0")))
                .andExpect(content().string(containsString("2 1.25 2.5")))
                .andExpect(content().string(containsString("2 3")));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("dk.dtu.scout.backend.controller.InstanceIntegrationTest#invalidExportPayloads")
        void exportInstance_rejectsInvalidPayloads(String label, Object payload, String expectedMessage) throws Exception {
            postExport(payload)
                .andExpect(status().isBadRequest())
                .andExpect(content().string(expectedMessage));
        }
    }

    private ResultActions postImport(Object payload) throws Exception {
        return mockMvc.perform(post("/api/instance/import")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(payload)));
    }

    private ResultActions postExport(Object payload) throws Exception {
        return mockMvc.perform(post("/api/instance/export")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(payload)));
    }

    private String json(Object payload) throws Exception {
        return objectMapper.writeValueAsString(payload);
    }

    private static Stream<Arguments> invalidImportPayloads() {
        return Stream.of(
            Arguments.of(
                "invalid header line",
                Map.of("content", String.join("\n",
                    "NAME: tiny",
                    "12345",
                    "TYPE: TSP",
                    "EDGE_WEIGHT_TYPE: EUC_2D",
                    "NODE_COORD_SECTION",
                    "1 0 0",
                    "2 1 1",
                    "EOF"
                )),
                "Invalid header line: 12345"
            ),
            Arguments.of(
                "missing content field",
                Map.of(),
                "Instance file content must be provided"
            ),
            Arguments.of(
                "blank content",
                Map.of("content", ""),
                "Instance file content must be provided"
            ),
            Arguments.of(
                "whitespace content",
                Map.of("content", "   "),
                "Instance file content must be provided"
            ),
            Arguments.of(
                "missing TYPE",
                Map.of("content", "NAME: tiny\n"),
                "Instance file must contain a TYPE field"
            ),
            Arguments.of(
                "blank TYPE",
                Map.of("content", "TYPE:   \n"),
                "Instance file must contain a TYPE field"
            ),
            Arguments.of(
                "unsupported TYPE",
                Map.of("content", "TYPE: ATSP\n"),
                "Unsupported instance TYPE: ATSP"
            ),
            Arguments.of(
                "TSP invalid coordinate line",
                Map.of("content", String.join("\n",
                    "NAME: tiny",
                    "TYPE: TSP",
                    "NODE_COORD_SECTION",
                    "1 2",
                    "EOF"
                )),
                "Invalid TSP coordinate line: 1 2"
            ),
            Arguments.of(
                "TSP unsupported edge weight type",
                Map.of("content", String.join("\n",
                    "NAME: tiny",
                    "TYPE: TSP",
                    "EDGE_WEIGHT_TYPE: GEO",
                    "NODE_COORD_SECTION",
                    "1 0 0",
                    "EOF"
                )),
                "Only EUC_2D edge weight type is supported, but got: GEO"
            ),
            Arguments.of(
                "TSP dimension mismatch",
                Map.of("content", String.join("\n",
                    "NAME: tiny",
                    "TYPE: TSP",
                    "DIMENSION: 3",
                    "NODE_COORD_SECTION",
                    "1 0 0",
                    "2 1 1",
                    "EOF"
                )),
                "DIMENSION is 3, but NODE_COORD_SECTION contains 2 nodes"
            ),
            Arguments.of(
                "VRP invalid coordinate line",
                Map.of("content", String.join("\n",
                    "NAME: tiny",
                    "TYPE: CVRP",
                    "CAPACITY: 10",
                    "NODE_COORD_SECTION",
                    "1 0",
                    "EOF"
                )),
                "Invalid coordinate line: 1 0"
            ),
            Arguments.of(
                "VRP invalid demand line",
                Map.of("content", String.join("\n",
                    "NAME: tiny",
                    "TYPE: CVRP",
                    "CAPACITY: 10",
                    "NODE_COORD_SECTION",
                    "1 0 0",
                    "DEMAND_SECTION",
                    "1",
                    "EOF"
                )),
                "Invalid demand line: 1"
            ),
            Arguments.of(
                "VRP unsupported edge weight type",
                Map.of("content", String.join("\n",
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
                )),
                "Only EUC_2D edge weight type is supported, but got: GEO"
            ),
            Arguments.of(
                "VRP missing capacity",
                Map.of("content", String.join("\n",
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
                )),
                "VRP file must contain a CAPACITY field"
            ),
            Arguments.of(
                "VRP missing demand section",
                Map.of("content", String.join("\n",
                    "NAME: tiny",
                    "TYPE: CVRP",
                    "CAPACITY: 10",
                    "NODE_COORD_SECTION",
                    "1 0 0",
                    "DEPOT_SECTION",
                    "1",
                    "-1",
                    "EOF"
                )),
                "VRP file must contain DEMAND_SECTION"
            ),
            Arguments.of(
                "VRP missing depot section",
                Map.of("content", String.join("\n",
                    "NAME: tiny",
                    "TYPE: CVRP",
                    "CAPACITY: 10",
                    "NODE_COORD_SECTION",
                    "1 0 0",
                    "DEMAND_SECTION",
                    "1 0",
                    "EOF"
                )),
                "VRP file must contain DEPOT_SECTION"
            ),
            Arguments.of(
                "VRP multiple depots",
                Map.of("content", String.join("\n",
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
                )),
                "Only single-depot CVRP instances are currently supported"
            ),
            Arguments.of(
                "VRP missing node section",
                Map.of("content", String.join("\n",
                    "NAME: tiny",
                    "TYPE: CVRP",
                    "CAPACITY: 10",
                    "DEMAND_SECTION",
                    "1 0",
                    "DEPOT_SECTION",
                    "1",
                    "-1",
                    "EOF"
                )),
                "VRP file must contain NODE_COORD_SECTION"
            ),
            Arguments.of(
                "VRP depot missing coordinates",
                Map.of("content", String.join("\n",
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
                )),
                "Depot node 2 is missing coordinates"
            ),
            Arguments.of(
                "VRP missing customer demand",
                Map.of("content", String.join("\n",
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
                )),
                "Missing demand for customer node 2"
            ),
            Arguments.of(
                "VRP invalid vehicle count",
                Map.of("content", String.join("\n",
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
                )),
                "VEHICLES must be positive"
            )
        );
    }

    private static Stream<Arguments> invalidExportPayloads() {
        return Stream.of(
            Arguments.of(
                "missing exportType",
                Map.of("name", "tiny"),
                "exportType must be provided"
            ),
            Arguments.of(
                "blank exportType",
                Map.of("exportType", "   "),
                "exportType must be provided"
            ),
            Arguments.of(
                "unsupported exportType",
                Map.of("exportType", "ATSP"),
                "Unsupported exportType: ATSP"
            ),
            Arguments.of(
                "TSP missing cities",
                Map.of("exportType", "TSP", "name", "tiny"),
                "tspInstance must contain a non-empty cities list"
            ),
            Arguments.of(
                "TSP empty cities",
                Map.of("exportType", "TSP", "cities", List.of()),
                "tspInstance must contain a non-empty cities list"
            ),
            Arguments.of(
                "TSP city not object",
                Map.of("exportType", "TSP", "cities", List.of("not-a-map")),
                "tspInstance city must be a map"
            ),
            Arguments.of(
                "TSP missing coordinate",
                Map.of("exportType", "TSP", "cities", List.of(Map.of("x", 1.0))),
                "Missing numeric value"
            ),
            Arguments.of(
                "VRP missing depot",
                Map.of(
                    "exportType", "VRP",
                    "capacity", 10,
                    "customers", List.of(Map.of("x", 1.0, "y", 1.0, "demand", 1.0))
                ),
                "vrpInstance must include depot coordinates"
            ),
            Arguments.of(
                "VRP depot not object",
                Map.of(
                    "exportType", "VRP",
                    "capacity", 10,
                    "depot", "not-a-map",
                    "customers", List.of(Map.of("x", 1.0, "y", 1.0, "demand", 1.0))
                ),
                "vrpInstance must include depot coordinates"
            ),
            Arguments.of(
                "VRP missing capacity",
                Map.of(
                    "exportType", "VRP",
                    "depot", Map.of("x", 0.0, "y", 0.0),
                    "customers", List.of(Map.of("x", 1.0, "y", 1.0, "demand", 1.0))
                ),
                "Missing numeric value"
            ),
            Arguments.of(
                "VRP customers not list",
                Map.of(
                    "exportType", "VRP",
                    "capacity", 10,
                    "depot", Map.of("x", 0.0, "y", 0.0),
                    "customers", "not-a-list"
                ),
                "vrpInstance must contain a non-empty customers list"
            ),
            Arguments.of(
                "VRP empty customers",
                Map.of(
                    "exportType", "VRP",
                    "capacity", 10,
                    "depot", Map.of("x", 0.0, "y", 0.0),
                    "customers", List.of()
                ),
                "vrpInstance must contain a non-empty customers list"
            ),
            Arguments.of(
                "VRP customer not object",
                Map.of(
                    "exportType", "VRP",
                    "capacity", 10,
                    "depot", Map.of("x", 0.0, "y", 0.0),
                    "customers", List.of("not-a-map")
                ),
                "vrpInstance customer must be a map"
            ),
            Arguments.of(
                "VRP missing customer demand",
                Map.of(
                    "exportType", "VRP",
                    "capacity", 10,
                    "depot", Map.of("x", 0.0, "y", 0.0),
                    "customers", List.of(Map.of("x", 1.0, "y", 1.0))
                ),
                "Missing numeric value"
            ),
            Arguments.of(
                "VRP negative capacity",
                Map.of(
                    "exportType", "VRP",
                    "capacity", -1,
                    "depot", Map.of("x", 0.0, "y", 0.0),
                    "customers", List.of(Map.of("x", 1.0, "y", 1.0, "demand", 1.0))
                ),
                "VRP capacity cannot be negative"
            ),
            Arguments.of(
                "VRP invalid vehicle count",
                Map.of(
                    "exportType", "VRP",
                    "capacity", 10,
                    "numberOfVehicles", 0,
                    "depot", Map.of("x", 0.0, "y", 0.0),
                    "customers", List.of(Map.of("x", 1.0, "y", 1.0, "demand", 1.0))
                ),
                "Number of vehicles must be positive"
            ),
            Arguments.of(
                "VRP null vehicle count",
                validVrpPayloadWithNullVehicleCount(),
                "Missing integer value"
            ),
            Arguments.of(
                "VRP negative demand",
                Map.of(
                    "exportType", "VRP",
                    "capacity", 10,
                    "depot", Map.of("x", 0.0, "y", 0.0),
                    "customers", List.of(Map.of("x", 1.0, "y", 1.0, "demand", -1.0))
                ),
                "VRP customer 1 demand cannot be negative"
            ),
            Arguments.of(
                "VRP demand exceeds capacity",
                Map.of(
                    "exportType", "VRP",
                    "capacity", 10,
                    "depot", Map.of("x", 0.0, "y", 0.0),
                    "customers", List.of(Map.of("x", 1.0, "y", 1.0, "demand", 11.0))
                ),
                "VRP customer 1 demand exceeds vehicle capacity"
            )
        );
    }

    private static String validTsp(boolean includeEof, boolean includeBlankLines) {
        StringBuilder builder = new StringBuilder();
        builder.append("NAME: tiny\n");
        builder.append("TYPE: TSP\n");
        builder.append("EDGE_WEIGHT_TYPE: EUC_2D\n");

        if (includeBlankLines) {
            builder.append("\n");
        }

        builder.append("NODE_COORD_SECTION\n");
        builder.append("1 0 0\n");

        if (includeBlankLines) {
            builder.append("\n");
        }

        builder.append("2 1 1");

        if (includeEof) {
            builder.append("\nEOF");
        }

        return builder.toString();
    }

    private static String validVrp(boolean includeEof, boolean includeBlankLines) {
        StringBuilder builder = new StringBuilder();
        builder.append("NAME: tiny\n");
        builder.append("TYPE: CVRP\n");
        builder.append("EDGE_WEIGHT_TYPE: EUC_2D\n");
        builder.append("CAPACITY: 10\n");

        if (includeBlankLines) {
            builder.append("\n");
        }

        builder.append("NODE_COORD_SECTION\n");
        builder.append("1 0 0\n");
        builder.append("2 1 1\n");

        if (includeBlankLines) {
            builder.append("\n");
        }

        builder.append("DEMAND_SECTION\n");
        builder.append("1 0\n");
        builder.append("2 1\n");
        builder.append("DEPOT_SECTION\n");
        builder.append("1\n");
        builder.append("-1");

        if (includeEof) {
            builder.append("\nEOF");
        }

        return builder.toString();
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

    private static Map<String, Object> validTspPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
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
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("exportType", "VRP");
        payload.put("name", "tiny");
        payload.put("comment", "");
        payload.put("capacity", 10);
        payload.put("numberOfVehicles", 1);
        payload.put("depot", Map.of("x", 0.0, "y", 0.0));
        payload.put("customers", List.of(Map.of("x", 1.0, "y", 1.0, "demand", 1.0)));
        return payload;
    }

    private static Map<String, Object> validVrpPayloadWithNullVehicleCount() {
        Map<String, Object> payload = validVrpPayload();
        payload.put("numberOfVehicles", null);
        return payload;
    }
}