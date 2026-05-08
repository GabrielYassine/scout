package dk.dtu.scout.backend.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dtu.scout.backend.dto.request.RunRequest;
import dk.dtu.scout.backend.dto.request.RuntimeStudyRequest;
import dk.dtu.scout.backend.service.ExecutionRegistry;
import dk.dtu.scout.backend.service.RunComponentFactory;
import dk.dtu.scout.problems.TSP;
import dk.dtu.scout.problems.VRP;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static dk.dtu.scout.backend.integrationtests.support.BackendJsonTestSupport.*;
import static dk.dtu.scout.backend.integrationtests.support.RunPrepareTestSupport.postRunPrepare;
import static dk.dtu.scout.backend.integrationtests.support.RunPrepareTestSupport.postRuntimeStudyPrepare;
import static dk.dtu.scout.backend.integrationtests.support.RunRequestFixtures.validRunPreparePayload;
import static dk.dtu.scout.backend.integrationtests.support.RunRequestFixtures.validRuntimeStudyPreparePayload;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RunPrepareIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExecutionRegistry executionRegistry;

    @Autowired
    private RunComponentFactory runComponentFactory;

    @Nested
    class StandardRunPreparation {

        @Test
        void prepareRun_generatesIdsStoresPreparedRunAndReusesSession() throws Exception {
            String sessionId = "prepare-session-run";

            String executionId = prepareRunAndGetExecutionId(sessionId);

            RunRequest stored = executionRegistry.consumePreparedRun(executionId, sessionId);

            assertEquals(sessionId, stored.sessionId());
            assertEquals(executionId, stored.runId());
            assertEquals("bitstring", stored.searchSpaceId());
            assertEquals(List.of("onemax"), stored.problemIds());
            assertEquals("bit-flip", stored.generatorId());
            assertEquals("max-evaluations", stored.stopConditionIds().getFirst());
        }

        @Test
        void prepareRun_generatesSessionWhenBlank() throws Exception {
            Map<String, Object> response = prepareRunAndReadResponse(validRunPreparePayload("   "));

            String sessionId = stringValue(response, "sessionId");
            String executionId = stringValue(response, "executionId");

            assertFalse(sessionId.isBlank());
            assertNotEquals("   ", sessionId);
            assertFalse(executionId.isBlank());

            RunRequest stored = executionRegistry.consumePreparedRun(executionId, sessionId);

            assertEquals(sessionId, stored.sessionId());
            assertEquals(executionId, stored.runId());
        }

        @Test
        void prepareRun_generatesSessionWhenMissing() throws Exception {
            Map<String, Object> response = prepareRunAndReadResponse(validRunPreparePayload(null));

            String sessionId = stringValue(response, "sessionId");
            String executionId = stringValue(response, "executionId");

            assertFalse(sessionId.isBlank());
            assertFalse(executionId.isBlank());

            RunRequest stored = executionRegistry.consumePreparedRun(executionId, sessionId);

            assertEquals(sessionId, stored.sessionId());
            assertEquals(executionId, stored.runId());
        }

        @Test
        void prepareRun_replacesOldPreparedRunForSameSession() throws Exception {
            String sessionId = "prepare-session-replace";

            String firstRunId = prepareRunAndGetExecutionId(sessionId);
            String secondRunId = prepareRunAndGetExecutionId(sessionId);

            assertNotEquals(firstRunId, secondRunId);
            assertPreparedRunMissing(firstRunId, sessionId);

            RunRequest stored = executionRegistry.consumePreparedRun(secondRunId, sessionId);

            assertEquals(secondRunId, stored.runId());
            assertEquals(sessionId, stored.sessionId());
        }

        @Test
        void prepareRun_acceptsTspProblemWhenInstanceIsProvided() throws Exception {
            String sessionId = "prepare-session-tsp";
            String executionId = prepareRunAndGetExecutionId(tspRunPayload(sessionId, Map.of("tspInstance", validTspInstancePayload())));

            RunRequest stored = executionRegistry.consumePreparedRun(executionId, sessionId);

            assertEquals(List.of("tsp"), stored.problemIds());
            assertEquals("permutation", stored.searchSpaceId());
        }

        @Test
        void prepareRun_acceptsVrpProblemWhenInstanceIsProvided() throws Exception {
            String sessionId = "prepare-session-vrp";
            String executionId = prepareRunAndGetExecutionId(vrpRunPayload(sessionId, Map.of("vrpInstance", validVrpInstancePayload())));

            RunRequest stored = executionRegistry.consumePreparedRun(executionId, sessionId);

            assertEquals(List.of("vrp"), stored.problemIds());
            assertEquals("route-list", stored.searchSpaceId());
        }

        @Test
        void prepareRun_rejectsMissingRunRequest() throws Exception {
            Map<String, Object> payload = preparePayload(
                    "prepare-session-missing-run-request",
                    "run",
                    null,
                    null
            );

            assertRunPrepareBadRequest(payload, "Run request must be provided");
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("dk.dtu.scout.backend.integrationtests.RunPrepareIntegrationTest#invalidRunPreparePayloads")
        void prepareRun_rejectsInvalidRunRequests(String label, Map<String, Object> payload, String expectedMessage) throws Exception {
            assertRunPrepareBadRequest(payload, expectedMessage);
        }
    }

    @Nested
    class RuntimeStudyPreparation {

        @Test
        void prepareRuntimeStudy_generatesIdsStoresPreparedStudyAndReusesSession() throws Exception {
            String sessionId = "prepare-session-study";

            String executionId = prepareRuntimeStudyAndGetExecutionId(sessionId);

            RuntimeStudyRequest stored = executionRegistry.consumePreparedStudy(executionId, sessionId);

            assertEquals(sessionId, stored.sessionId());
            assertEquals(executionId, stored.studyId());
            assertEquals("bitstring", stored.searchSpaceId());
            assertEquals("onemax", stored.problemId());
            assertEquals(List.of(5, 10), stored.problemSizes());
        }

        @Test
        void prepareRuntimeStudy_acceptsHyphenatedExecutionType() throws Exception {
            String sessionId = "prepare-session-study-hyphen";

            Map<String, Object> payload = validRuntimeStudyPreparePayload(sessionId);
            payload.put("executionType", "runtime-study");

            String executionId = prepareRuntimeStudyAndGetExecutionId(payload);

            RuntimeStudyRequest stored = executionRegistry.consumePreparedStudy(executionId, sessionId);

            assertEquals(executionId, stored.studyId());
        }

        @Test
        void prepareRuntimeStudy_rejectsMissingRuntimeStudyRequest() throws Exception {
            Map<String, Object> payload = preparePayload(
                    "prepare-session-missing-study-request",
                    "runtimeStudy",
                    null,
                    null
            );

            assertRuntimeStudyPrepareBadRequest(payload, "Runtime study request must be provided");
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("dk.dtu.scout.backend.integrationtests.RunPrepareIntegrationTest#invalidRuntimeStudyPreparePayloads")
        void prepareRuntimeStudy_rejectsInvalidRuntimeStudyRequests(String label, Map<String, Object> payload, String expectedMessage) throws Exception {
            assertRuntimeStudyPrepareBadRequest(payload, expectedMessage);
        }
    }

    @Nested
    class InvalidExecutionType {

        @Test
        void prepareRun_rejectsUnsupportedExecutionType() throws Exception {
            Map<String, Object> payload = validRunPreparePayload("prepare-session-invalid-type");
            payload.put("executionType", "something-else");

            assertRunPrepareBadRequest(payload, "executionType must be either 'run' or 'runtimeStudy'");
        }

        @Test
        void prepareRun_rejectsMissingExecutionType() throws Exception {
            Map<String, Object> payload = validRunPreparePayload("prepare-session-missing-type");
            payload.put("executionType", null);

            assertRunPrepareBadRequest(payload, "executionType must be either 'run' or 'runtimeStudy'");
        }
    }

    @Nested
    class ProblemSpecificParamMapping {

        @Test
        void createProblem_mapsTspInstanceParams() {
            TSP tspProblem = assertInstanceOf(
                    TSP.class,
                    runComponentFactory.createProblem("tsp", 2, Map.of("tspInstance", validTspInstancePayload()))
            );

            assertNotNull(tspProblem.getInstance());
            assertEquals("tiny", tspProblem.getInstance().getName());
            assertEquals(2, tspProblem.getInstance().getDimension());
        }

        @Test
        void createProblem_mapsVrpInstanceParams() {
            VRP vrpProblem = assertInstanceOf(
                    VRP.class,
                    runComponentFactory.createProblem("vrp", 0, Map.of("vrpInstance", validVrpInstancePayload()))
            );

            assertNotNull(vrpProblem.getInstance());
            assertEquals("tiny", vrpProblem.getInstance().getName());
            assertEquals(1, vrpProblem.getInstance().getCustomerCount());
        }
    }

    private String prepareRunAndGetExecutionId(String sessionId) throws Exception {
        return prepareRunAndGetExecutionId(validRunPreparePayload(sessionId));
    }

    private String prepareRunAndGetExecutionId(Map<String, Object> payload) throws Exception {
        return stringValue(prepareRunAndReadResponse(payload), "executionId");
    }

    private Map<String, Object> prepareRunAndReadResponse(Map<String, Object> payload) throws Exception {
        MvcResult result = postRunPrepare(mockMvc, objectMapper, payload)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").isString())
                .andExpect(jsonPath("$.executionId").isString())
                .andReturn();

        return readJsonObject(objectMapper, result);
    }

    private String prepareRuntimeStudyAndGetExecutionId(String sessionId) throws Exception {
        return prepareRuntimeStudyAndGetExecutionId(validRuntimeStudyPreparePayload(sessionId));
    }

    private String prepareRuntimeStudyAndGetExecutionId(Map<String, Object> payload) throws Exception {
        MvcResult result = postRuntimeStudyPrepare(mockMvc, objectMapper, payload)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").isString())
                .andExpect(jsonPath("$.executionId").isString())
                .andReturn();

        return stringValue(readJsonObject(objectMapper, result), "executionId");
    }

    private void assertRunPrepareBadRequest(Map<String, Object> payload, String expectedMessage) throws Exception {
        postRunPrepare(mockMvc, objectMapper, payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    private void assertRuntimeStudyPrepareBadRequest(Map<String, Object> payload, String expectedMessage) throws Exception {
        postRuntimeStudyPrepare(mockMvc, objectMapper, payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    private void assertPreparedRunMissing(String runId, String sessionId) {
        try {
            executionRegistry.consumePreparedRun(runId, sessionId);
            throw new AssertionError("Expected old prepared run to be removed");
        } catch (IllegalArgumentException expected) {
            assertEquals("Prepared run was not found or has already been started", expected.getMessage());
        }
    }

    private static Stream<Arguments> invalidRunPreparePayloads() {
        return Stream.of(
                invalidRun("missing search space", "searchSpaceId", "", "Search space must be specified"),
                invalidRun("missing problem", "problemIds", List.of(), "Problem must be specified"),
                invalidRun("missing generator", "generatorId", "", "Generator must be specified"),
                invalidRun("missing population model", "populationModelId", "", "Population model must be specified"),
                invalidRun("missing selection rule", "selectionRuleId", "", "Selection rule must be specified"),
                invalidRun("missing parent selection rule", "parentSelectionRuleId", "", "Parent selection rule must be specified"),
                invalidRun("missing stop condition", "stopConditionIds", List.of(), "Stop condition must be specified"),
                invalidRun("invalid runTimes", "runTimes", 0, "runTimes must be positive"),
                invalidRun("invalid logEveryEvaluations", "logEveryEvaluations", -1, "logEveryEvaluations must be zero or positive"),

                Arguments.of("TSP missing instance", tspRunPayload("invalid-run-tsp", Map.of()), "TSP problem requires a TSP instance"),
                Arguments.of("TSP instance not map", tspRunPayload("invalid-run-tsp", Map.of("tspInstance", "not-a-map")), "Invalid TSP instance: tspInstance must be a map"),
                Arguments.of("TSP invalid instance", tspRunPayload("invalid-run-tsp", Map.of("tspInstance", Map.of("name", "tiny"))), "Invalid TSP instance: tspInstance must contain a non-empty cities list"),

                Arguments.of("VRP missing instance", vrpRunPayload("invalid-run-vrp", Map.of()), "VRP problem requires a VRP instance"),
                Arguments.of("VRP instance not map", vrpRunPayload("invalid-run-vrp", Map.of("vrpInstance", "not-a-map")), "Invalid VRP instance: vrpInstance must be a map"),
                Arguments.of("VRP invalid instance", vrpRunPayload("invalid-run-vrp", Map.of("vrpInstance", Map.of("name", "tiny"))), "Invalid VRP instance: Missing numeric value"),

                invalidRun("null search space", "searchSpaceId", null, "Search space must be specified"),
                invalidRun("null problem list", "problemIds", null, "Problem must be specified"),
                invalidRun("null generator", "generatorId", null, "Generator must be specified"),
                invalidRun("null population model", "populationModelId", null, "Population model must be specified"),
                invalidRun("null selection rule", "selectionRuleId", null, "Selection rule must be specified"),
                invalidRun("null parent selection rule", "parentSelectionRuleId", null, "Parent selection rule must be specified"),
                invalidRun("null stop condition list", "stopConditionIds", null, "Stop condition must be specified"),

                Arguments.of("TSP null problem params", tspRunPayload("invalid-run-tsp", null), "TSP problem requires a TSP instance"),
                Arguments.of("VRP null problem params", vrpRunPayload("invalid-run-vrp", null), "VRP problem requires a VRP instance")
        );
    }

    private static Stream<Arguments> invalidRuntimeStudyPreparePayloads() {
        return Stream.of(
                invalidStudy("missing search space", "searchSpaceId", "", "Search space must be specified"),
                invalidStudy("missing problem", "problemId", "", "Problem must be specified"),
                invalidStudy("TSP runtime study", "problemId", "tsp", "Runtime study currently supports theoretical size-based problems only"),
                invalidStudy("VRP runtime study", "problemId", "vrp", "Runtime study currently supports theoretical size-based problems only"),
                invalidStudy("missing generator", "generatorId", "", "Generator must be specified"),
                invalidStudy("missing population model", "populationModelId", "", "Population model must be specified"),
                invalidStudy("missing selection rule", "selectionRuleId", "", "Selection rule must be specified"),
                invalidStudy("missing parent selection rule", "parentSelectionRuleId", "", "Parent selection rule must be specified"),
                invalidStudy("empty stop condition", "stopConditionIds", List.of(), "Stop condition must contain 'optimum-reached' and cannot be empty"),
                invalidStudy("missing optimum-reached stop condition", "stopConditionIds", List.of("max-evaluations"), "Stop condition must contain 'optimum-reached' and cannot be empty"),
                invalidStudy("empty problem sizes", "problemSizes", List.of(), "At least one problem size must be specified"),
                invalidStudy("null problem size", "problemSizes", problemSizesWithNull(), "All problem sizes must be positive"),
                invalidStudy("non-positive problem size", "problemSizes", List.of(5, 0), "All problem sizes must be positive"),
                invalidStudy("invalid repetitions", "repetitionsPerSize", 0, "repetitionsPerSize must be positive"),
                invalidStudy("invalid seed", "seed", 0, "seed must be positive"),
                invalidStudy("null search space", "searchSpaceId", null, "Search space must be specified"),
                invalidStudy("null problem", "problemId", null, "Problem must be specified"),
                invalidStudy("null generator", "generatorId", null, "Generator must be specified"),
                invalidStudy("null population model", "populationModelId", null, "Population model must be specified"),
                invalidStudy("null selection rule", "selectionRuleId", null, "Selection rule must be specified"),
                invalidStudy("null parent selection rule", "parentSelectionRuleId", null, "Parent selection rule must be specified"),
                invalidStudy("null stop condition list", "stopConditionIds", null, "Stop condition must contain 'optimum-reached' and cannot be empty"),
                invalidStudy("null problem sizes", "problemSizes", null, "At least one problem size must be specified")
        );
    }

    private static Arguments invalidRun(String label, String key, Object value, String expectedMessage) {
        return Arguments.of(label, runPayload("invalid-run-" + key, run -> run.put(key, value)), expectedMessage);
    }

    private static Arguments invalidStudy(String label, String key, Object value, String expectedMessage) {
        return Arguments.of(label, studyPayload("invalid-study-" + key, study -> study.put(key, value)), expectedMessage);
    }

    private static Map<String, Object> runPayload(String sessionId, Consumer<Map<String, Object>> configure) {
        Map<String, Object> payload = validRunPreparePayload(sessionId);
        configure.accept(mapValue(payload, "runRequest"));
        return payload;
    }

    private static Map<String, Object> studyPayload(String sessionId, Consumer<Map<String, Object>> configure) {
        Map<String, Object> payload = validRuntimeStudyPreparePayload(sessionId);
        configure.accept(mapValue(payload, "runtimeStudyRequest"));
        return payload;
    }

    private static Map<String, Object> tspRunPayload(String sessionId, Map<String, Object> problemParams) {
        return runPayload(sessionId, run -> {
            run.put("searchSpaceId", "permutation");
            run.put("searchSpaceParams", Map.of("n", 2));
            run.put("problemIds", List.of("tsp"));
            run.put("problemParams", problemParams);
            run.put("generatorId", "2opt");
            run.put("generatorParams", Map.of());
            run.put("parentSelectionRuleId", "random-parents");
        });
    }

    private static Map<String, Object> vrpRunPayload(String sessionId, Map<String, Object> problemParams) {
        return runPayload(sessionId, run -> {
            run.put("searchSpaceId", "route-list");
            run.put("searchSpaceParams", Map.of());
            run.put("problemIds", List.of("vrp"));
            run.put("problemParams", problemParams);
            run.put("generatorId", "route-list-relocate");
            run.put("generatorParams", Map.of());
            run.put("parentSelectionRuleId", "random-parents");
        });
    }

    private static Map<String, Object> preparePayload(
            String sessionId,
            String executionType,
            Object runRequest,
            Object runtimeStudyRequest
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("executionType", executionType);
        payload.put("runRequest", runRequest);
        payload.put("runtimeStudyRequest", runtimeStudyRequest);
        return payload;
    }

    private static List<Integer> problemSizesWithNull() {
        List<Integer> sizes = new ArrayList<>();
        sizes.add(5);
        sizes.add(null);
        return sizes;
    }

    private static Map<String, Object> validTspInstancePayload() {
        Map<String, Object> tsp = new LinkedHashMap<>();
        tsp.put("name", "tiny");
        tsp.put("comment", "");
        tsp.put("cities", List.of(
                Map.of("x", 0.0, "y", 0.0),
                Map.of("x", 1.0, "y", 1.0)
        ));
        return tsp;
    }

    private static Map<String, Object> validVrpInstancePayload() {
        Map<String, Object> vrp = new LinkedHashMap<>();
        vrp.put("name", "tiny");
        vrp.put("comment", "");
        vrp.put("capacity", 10);
        vrp.put("numberOfVehicles", 1);
        vrp.put("depot", Map.of("x", 0.0, "y", 0.0));
        vrp.put("customers", List.of(
                Map.of("x", 1.0, "y", 1.0, "demand", 1.0)
        ));
        return vrp;
    }
}