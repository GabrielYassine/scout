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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
            String requestedSessionId = "prepare-session-run";

            MvcResult result = postPrepare(validRunPreparePayload(requestedSessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(requestedSessionId))
                .andExpect(jsonPath("$.executionId").isString())
                .andReturn();

            Map<String, Object> response = readResponse(result);
            String executionId = response.get("executionId").toString();

            assertFalse(executionId.isBlank());

            RunRequest stored = executionRegistry.consumePreparedRun(executionId, requestedSessionId);

            assertEquals(requestedSessionId, stored.sessionId());
            assertEquals(executionId, stored.runId());
            assertEquals("bitstring", stored.searchSpaceId());
            assertEquals(List.of("onemax"), stored.problemIds());
            assertEquals("bit-flip", stored.generatorId());
            assertEquals("max-evaluations", stored.stopConditionIds().getFirst());
        }

        @Test
        void prepareRun_generatesSessionWhenBlank() throws Exception {
            MvcResult result = postPrepare(validRunPreparePayload("   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").isString())
                .andExpect(jsonPath("$.executionId").isString())
                .andReturn();

            Map<String, Object> response = readResponse(result);
            String sessionId = response.get("sessionId").toString();
            String executionId = response.get("executionId").toString();

            assertFalse(sessionId.isBlank());
            assertNotEquals("   ", sessionId);
            assertFalse(executionId.isBlank());

            RunRequest stored = executionRegistry.consumePreparedRun(executionId, sessionId);

            assertEquals(sessionId, stored.sessionId());
            assertEquals(executionId, stored.runId());
        }

        @Test
        void prepareRun_generatesSessionWhenMissing() throws Exception {
            MvcResult result = postPrepare(validRunPreparePayload(null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").isString())
                .andExpect(jsonPath("$.executionId").isString())
                .andReturn();

            Map<String, Object> response = readResponse(result);
            String sessionId = response.get("sessionId").toString();
            String executionId = response.get("executionId").toString();

            assertFalse(sessionId.isBlank());
            assertFalse(executionId.isBlank());

            RunRequest stored = executionRegistry.consumePreparedRun(executionId, sessionId);

            assertEquals(sessionId, stored.sessionId());
            assertEquals(executionId, stored.runId());
        }

        @Test
        void prepareRun_replacesOldPreparedRunForSameSession() throws Exception {
            String sessionId = "prepare-session-replace";

            MvcResult first = postPrepare(validRunPreparePayload(sessionId)).andExpect(status().isOk()).andReturn();
            String firstRunId = readResponse(first).get("executionId").toString();

            MvcResult second = postPrepare(validRunPreparePayload(sessionId)).andExpect(status().isOk()).andReturn();
            String secondRunId = readResponse(second).get("executionId").toString();

            assertNotEquals(firstRunId, secondRunId);
            assertPreparedRunMissing(firstRunId, sessionId);
            RunRequest stored = executionRegistry.consumePreparedRun(secondRunId, sessionId);
            assertEquals(secondRunId, stored.runId());
            assertEquals(sessionId, stored.sessionId());
        }

        @Test
        void prepareRun_acceptsTspProblemWhenInstanceIsProvided() throws Exception {
            Map<String, Object> payload = validRunPreparePayload("prepare-session-tsp");

            @SuppressWarnings("unchecked")
            Map<String, Object> runRequest = (Map<String, Object>) payload.get("runRequest");

            runRequest.put("searchSpaceId", "permutation");
            runRequest.put("searchSpaceParams", Map.of("n", 2));
            runRequest.put("problemIds", List.of("tsp"));
            runRequest.put("problemParams", Map.of("tspInstance", validTspInstancePayload()));
            runRequest.put("generatorId", "2opt");
            runRequest.put("generatorParams", Map.of());
            runRequest.put("parentSelectionRuleId", "random-parents");
            runRequest.put("observerIds", List.of("fitness", "tour"));

            MvcResult result = postPrepare(payload).andExpect(status().isOk()).andReturn();

            Map<String, Object> response = readResponse(result);
            String executionId = response.get("executionId").toString();

            RunRequest stored = executionRegistry.consumePreparedRun(executionId, "prepare-session-tsp");

            assertEquals(List.of("tsp"), stored.problemIds());
            assertEquals("permutation", stored.searchSpaceId());
        }

        @Test
        void prepareRun_acceptsVrpProblemWhenInstanceIsProvided() throws Exception {
            Map<String, Object> payload = validRunPreparePayload("prepare-session-vrp");

            @SuppressWarnings("unchecked")
            Map<String, Object> runRequest = (Map<String, Object>) payload.get("runRequest");

            runRequest.put("searchSpaceId", "route-list");
            runRequest.put("searchSpaceParams", Map.of());
            runRequest.put("problemIds", List.of("vrp"));
            runRequest.put("problemParams", Map.of("vrpInstance", validVrpInstancePayload()));
            runRequest.put("generatorId", "route-list-relocate");
            runRequest.put("generatorParams", Map.of());
            runRequest.put("parentSelectionRuleId", "random-parents");
            runRequest.put("observerIds", List.of("fitness"));

            MvcResult result = postPrepare(payload).andExpect(status().isOk()).andReturn();

            Map<String, Object> response = readResponse(result);
            String executionId = response.get("executionId").toString();

            RunRequest stored = executionRegistry.consumePreparedRun(executionId, "prepare-session-vrp");

            assertEquals(List.of("vrp"), stored.problemIds());
            assertEquals("route-list", stored.searchSpaceId());
        }

        @Test
        void prepareRun_rejectsMissingRunRequest() throws Exception {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sessionId", "prepare-session-missing-run-request");
            payload.put("executionType", "run");
            payload.put("runRequest", null);
            payload.put("runtimeStudyRequest", null);

            postPrepare(payload).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Run request must be provided"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("dk.dtu.scout.backend.integrationtests.RunPrepareIntegrationTest#invalidRunPreparePayloads")
        void prepareRun_rejectsInvalidRunRequests(String label, Map<String, Object> payload, String expectedMessage) throws Exception {
            postPrepare(payload).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(expectedMessage));
        }
    }

    @Nested
    class RuntimeStudyPreparation {

        @Test
        void prepareRuntimeStudy_generatesIdsStoresPreparedStudyAndReusesSession() throws Exception {
            String requestedSessionId = "prepare-session-study";

            MvcResult result = postPrepare(validRuntimeStudyPreparePayload(requestedSessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(requestedSessionId))
                    .andExpect(jsonPath("$.executionId").isString())
                    .andReturn();

            Map<String, Object> response = readResponse(result);
            String executionId = response.get("executionId").toString();

            assertFalse(executionId.isBlank());

            RuntimeStudyRequest stored = executionRegistry.consumePreparedStudy(executionId, requestedSessionId);

            assertEquals(requestedSessionId, stored.sessionId());
            assertEquals(executionId, stored.studyId());
            assertEquals("bitstring", stored.searchSpaceId());
            assertEquals("onemax", stored.problemId());
            assertEquals(List.of(5, 10), stored.problemSizes());
        }

        @Test
        void prepareRuntimeStudy_acceptsHyphenatedExecutionType() throws Exception {
            Map<String, Object> payload = validRuntimeStudyPreparePayload("prepare-session-study-hyphen");
            payload.put("executionType", "runtime-study");

            MvcResult result = postPrepare(payload).andExpect(status().isOk()).andReturn();

            Map<String, Object> response = readResponse(result);
            String executionId = response.get("executionId").toString();

            RuntimeStudyRequest stored = executionRegistry.consumePreparedStudy(executionId, "prepare-session-study-hyphen");

            assertEquals(executionId, stored.studyId());
        }

        @Test
        void prepareRuntimeStudy_rejectsMissingRuntimeStudyRequest() throws Exception {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sessionId", "prepare-session-missing-study-request");
            payload.put("executionType", "runtimeStudy");
            payload.put("runRequest", null);
            payload.put("runtimeStudyRequest", null);

            postPrepare(payload).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Runtime study request must be provided"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("dk.dtu.scout.backend.integrationtests.RunPrepareIntegrationTest#invalidRuntimeStudyPreparePayloads")
        void prepareRuntimeStudy_rejectsInvalidRuntimeStudyRequests(String label, Map<String, Object> payload, String expectedMessage) throws Exception {
            postPrepare(payload).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(expectedMessage));
        }
    }

    @Nested
    class InvalidExecutionType {

        @Test
        void prepareRun_rejectsUnsupportedExecutionType() throws Exception {
            Map<String, Object> payload = validRunPreparePayload("prepare-session-invalid-type");
            payload.put("executionType", "something-else");

            postPrepare(payload).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("executionType must be either 'run' or 'runtimeStudy'"));
        }

        @Test
        void prepareRun_rejectsMissingExecutionType() throws Exception {
            Map<String, Object> payload = validRunPreparePayload("prepare-session-missing-type");
            payload.put("executionType", null);

            postPrepare(payload).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("executionType must be either 'run' or 'runtimeStudy'"));
        }
    }

    @Nested
    class ProblemSpecificParamMapping {

        @Test
        void createProblem_mapsTspInstanceParams() {
            Map<String, Object> params = Map.of("tspInstance", validTspInstancePayload());

            Object problem = runComponentFactory.createProblem("tsp", 2, params);

            TSP tspProblem = assertInstanceOf(TSP.class, problem);
            assertNotNull(tspProblem.getInstance());
            assertEquals("tiny", tspProblem.getInstance().getName());
            assertEquals(2, tspProblem.getInstance().getDimension());
        }

        @Test
        void createProblem_mapsVrpInstanceParams() {
            Map<String, Object> params = Map.of("vrpInstance", validVrpInstancePayload());

            Object problem = runComponentFactory.createProblem("vrp", 0, params);

            VRP vrpProblem = assertInstanceOf(VRP.class, problem);
            assertNotNull(vrpProblem.getInstance());
            assertEquals("tiny", vrpProblem.getInstance().getName());
            assertEquals(1, vrpProblem.getInstance().getCustomerCount());
        }
    }

    private ResultActions postPrepare(Object payload) throws Exception {
        return mockMvc.perform(post("/api/run/prepare").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload)));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readResponse(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
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
            Arguments.of("missing search space", invalidRunPayload("searchSpaceId", ""), "Search space must be specified"),
            Arguments.of("missing problem", invalidRunPayload("problemIds", List.of()), "Problem must be specified"),
            Arguments.of("missing generator", invalidRunPayload("generatorId", ""), "Generator must be specified"),
            Arguments.of("missing population model", invalidRunPayload("populationModelId", ""), "Population model must be specified"),
            Arguments.of("missing selection rule", invalidRunPayload("selectionRuleId", ""), "Selection rule must be specified"),
            Arguments.of("missing parent selection rule", invalidRunPayload("parentSelectionRuleId", ""), "Parent selection rule must be specified"),
            Arguments.of("missing stop condition", invalidRunPayload("stopConditionIds", List.of()), "Stop condition must be specified"),
            Arguments.of("invalid runTimes", invalidRunPayload("runTimes", 0), "runTimes must be positive"),
            Arguments.of("invalid logEveryIterations", invalidRunPayload("logEveryIterations", -1), "logEveryIterations must be zero or positive"),
            Arguments.of("TSP missing instance", tspRunPayload(Map.of()), "TSP problem requires a TSP instance"),
            Arguments.of("TSP instance not map", tspRunPayload(Map.of("tspInstance", "not-a-map")), "Invalid TSP instance: tspInstance must be a map"),
            Arguments.of("TSP invalid instance", tspRunPayload(Map.of("tspInstance", Map.of("name", "tiny"))), "Invalid TSP instance: tspInstance must contain a non-empty cities list"),
            Arguments.of("VRP missing instance", vrpRunPayload(Map.of()), "VRP problem requires a VRP instance"),
            Arguments.of("VRP instance not map", vrpRunPayload(Map.of("vrpInstance", "not-a-map")), "Invalid VRP instance: vrpInstance must be a map"),
            Arguments.of("VRP invalid instance", vrpRunPayload(Map.of("vrpInstance", Map.of("name", "tiny"))), "Invalid VRP instance: Missing numeric value"),
            Arguments.of("null search space", invalidRunPayload("searchSpaceId", null), "Search space must be specified"),
            Arguments.of("null problem list", invalidRunPayload("problemIds", null), "Problem must be specified"),
            Arguments.of("null generator", invalidRunPayload("generatorId", null), "Generator must be specified"),
            Arguments.of("null population model", invalidRunPayload("populationModelId", null), "Population model must be specified"),
            Arguments.of("null selection rule", invalidRunPayload("selectionRuleId", null), "Selection rule must be specified"),
            Arguments.of("null parent selection rule", invalidRunPayload("parentSelectionRuleId", null), "Parent selection rule must be specified"),
            Arguments.of("null stop condition list", invalidRunPayload("stopConditionIds", null), "Stop condition must be specified"),
            Arguments.of("TSP null problem params", tspRunPayload(null), "TSP problem requires a TSP instance"),
            Arguments.of("VRP null problem params", vrpRunPayload(null), "VRP problem requires a VRP instance")
        );
    }

    private static Stream<Arguments> invalidRuntimeStudyPreparePayloads() {
        return Stream.of(
            Arguments.of("missing search space", invalidRuntimeStudyPayload("searchSpaceId", ""), "Search space must be specified"),
            Arguments.of("missing problem", invalidRuntimeStudyPayload("problemId", ""), "Problem must be specified"),
            Arguments.of("TSP runtime study", invalidRuntimeStudyPayload("problemId", "tsp"), "Runtime study currently supports theoretical size-based problems only"),
            Arguments.of("VRP runtime study", invalidRuntimeStudyPayload("problemId", "vrp"), "Runtime study currently supports theoretical size-based problems only"),
            Arguments.of("missing generator", invalidRuntimeStudyPayload("generatorId", ""), "Generator must be specified"),
            Arguments.of("missing population model", invalidRuntimeStudyPayload("populationModelId", ""), "Population model must be specified"),
            Arguments.of("missing selection rule", invalidRuntimeStudyPayload("selectionRuleId", ""), "Selection rule must be specified"),
            Arguments.of("missing parent selection rule", invalidRuntimeStudyPayload("parentSelectionRuleId", ""), "Parent selection rule must be specified"),
            Arguments.of("empty stop condition", invalidRuntimeStudyPayload("stopConditionIds", List.of()), "Stop condition must contain 'optimum-reached' and cannot be empty"),
            Arguments.of("missing optimum-reached stop condition", invalidRuntimeStudyPayload("stopConditionIds", List.of("max-evaluations")), "Stop condition must contain 'optimum-reached' and cannot be empty"),
            Arguments.of("empty problem sizes", invalidRuntimeStudyPayload("problemSizes", List.of()), "At least one problem size must be specified"),
            Arguments.of("null problem size", invalidRuntimeStudyPayload("problemSizes", problemSizesWithNull()), "All problem sizes must be positive"),
            Arguments.of("non-positive problem size", invalidRuntimeStudyPayload("problemSizes", List.of(5, 0)), "All problem sizes must be positive"),
            Arguments.of("invalid repetitions", invalidRuntimeStudyPayload("repetitionsPerSize", 0), "repetitionsPerSize must be positive"),
            Arguments.of("invalid seed", invalidRuntimeStudyPayload("seed", 0), "seed must be positive"),
            Arguments.of("null search space", invalidRuntimeStudyPayload("searchSpaceId", null), "Search space must be specified"),
            Arguments.of("null problem", invalidRuntimeStudyPayload("problemId", null), "Problem must be specified"),
            Arguments.of("null generator", invalidRuntimeStudyPayload("generatorId", null), "Generator must be specified"),
            Arguments.of("null population model", invalidRuntimeStudyPayload("populationModelId", null), "Population model must be specified"),
            Arguments.of("null selection rule", invalidRuntimeStudyPayload("selectionRuleId", null), "Selection rule must be specified"),
            Arguments.of("null parent selection rule", invalidRuntimeStudyPayload("parentSelectionRuleId", null), "Parent selection rule must be specified"),
            Arguments.of("null stop condition list", invalidRuntimeStudyPayload("stopConditionIds", null), "Stop condition must contain 'optimum-reached' and cannot be empty"),
            Arguments.of("null problem sizes", invalidRuntimeStudyPayload("problemSizes", null), "At least one problem size must be specified")
        );
    }

    private static Map<String, Object> invalidRunPayload(String key, Object value) {
        Map<String, Object> payload = validRunPreparePayload("invalid-run-" + key);

        @SuppressWarnings("unchecked")
        Map<String, Object> runRequest = (Map<String, Object>) payload.get("runRequest");

        runRequest.put(key, value);
        return payload;
    }

    private static Map<String, Object> invalidRuntimeStudyPayload(String key, Object value) {
        Map<String, Object> payload = validRuntimeStudyPreparePayload("invalid-study-" + key);

        @SuppressWarnings("unchecked")
        Map<String, Object> studyRequest = (Map<String, Object>) payload.get("runtimeStudyRequest");

        studyRequest.put(key, value);
        return payload;
    }

    private static Map<String, Object> tspRunPayload(Map<String, Object> problemParams) {
        Map<String, Object> payload = validRunPreparePayload("invalid-run-tsp");

        @SuppressWarnings("unchecked")
        Map<String, Object> runRequest = (Map<String, Object>) payload.get("runRequest");

        runRequest.put("searchSpaceId", "permutation");
        runRequest.put("searchSpaceParams", Map.of("n", 2));
        runRequest.put("problemIds", List.of("tsp"));
        runRequest.put("problemParams", problemParams);
        runRequest.put("generatorId", "2opt");
        runRequest.put("generatorParams", Map.of());
        runRequest.put("parentSelectionRuleId", "random-parents");

        return payload;
    }

    private static Map<String, Object> vrpRunPayload(Map<String, Object> problemParams) {
        Map<String, Object> payload = validRunPreparePayload("invalid-run-vrp");

        @SuppressWarnings("unchecked")
        Map<String, Object> runRequest = (Map<String, Object>) payload.get("runRequest");

        runRequest.put("searchSpaceId", "route-list");
        runRequest.put("searchSpaceParams", Map.of());
        runRequest.put("problemIds", List.of("vrp"));
        runRequest.put("problemParams", problemParams);
        runRequest.put("generatorId", "route-list-relocate");
        runRequest.put("generatorParams", Map.of());
        runRequest.put("parentSelectionRuleId", "random-parents");

        return payload;
    }

    private static List<Integer> problemSizesWithNull() {
        List<Integer> sizes = new ArrayList<>();
        sizes.add(5);
        sizes.add(null);
        return sizes;
    }

    private static Map<String, Object> validRunPreparePayload(String sessionId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("executionType", "run");
        payload.put("runRequest", validRunRequestPayload());
        payload.put("runtimeStudyRequest", null);
        return payload;
    }

    private static Map<String, Object> validRunRequestPayload() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("searchSpaceId", "bitstring");
        request.put("searchSpaceParams", Map.of("n", 10));
        request.put("problemIds", List.of("onemax"));
        request.put("problemParams", Map.of());
        request.put("generatorId", "bit-flip");
        request.put("generatorParams", Map.of("flipProbability", "1/n"));
        request.put("populationModelId", "mu-lambda");
        request.put("populationModelParams", Map.of("mu", 1, "lambda", 1));
        request.put("selectionRuleId", "mu-plus-lambda");
        request.put("selectionRuleParams", Map.of());
        request.put("parentSelectionRuleId", "elitist-parents");
        request.put("parentSelectionRuleParams", Map.of());
        request.put("crossoverId", null);
        request.put("crossoverParams", null);
        request.put("observerIds", List.of("fitness"));
        request.put("observerParams", Map.of());
        request.put("stopConditionIds", List.of("max-evaluations"));
        request.put("stopConditionParams", Map.of("maxEvaluations", 10));
        request.put("seed", 1234L);
        request.put("runTimes", 1);
        request.put("sessionId", null);
        request.put("runId", null);
        request.put("logEveryIterations", 1);
        request.put("wsUpdateEveryIterations", 1);
        return request;
    }

    private static Map<String, Object> validRuntimeStudyPreparePayload(String sessionId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("executionType", "runtimeStudy");
        payload.put("runRequest", null);
        payload.put("runtimeStudyRequest", validRuntimeStudyRequestPayload());
        return payload;
    }

    private static Map<String, Object> validRuntimeStudyRequestPayload() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("studyId", null);
        request.put("sessionId", null);
        request.put("searchSpaceId", "bitstring");
        request.put("searchSpaceParams", Map.of("n", 10));
        request.put("problemId", "onemax");
        request.put("problemParams", Map.of());
        request.put("generatorId", "bit-flip");
        request.put("generatorParams", Map.of("flipProbability", "1/n"));
        request.put("selectionRuleId", "mu-plus-lambda");
        request.put("selectionRuleParams", Map.of());
        request.put("populationModelId", "mu-lambda");
        request.put("populationModelParams", Map.of("mu", 1, "lambda", 1));
        request.put("parentSelectionRuleId", "elitist-parents");
        request.put("parentSelectionRuleParams", Map.of());
        request.put("crossoverId", null);
        request.put("crossoverParams", null);
        request.put("stopConditionIds", List.of("optimum-reached"));
        request.put("stopConditionParams", Map.of());
        request.put("seed", 1234L);
        request.put("problemSizes", List.of(5, 10));
        request.put("repetitionsPerSize", 1);
        return request;
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

