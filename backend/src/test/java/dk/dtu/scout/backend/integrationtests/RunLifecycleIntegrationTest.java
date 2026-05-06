package dk.dtu.scout.backend.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dtu.scout.backend.dto.request.StartPreparedExecutionRequest;
import dk.dtu.scout.backend.dto.ws.RunWsPayload;
import dk.dtu.scout.backend.dto.ws.RuntimeStudyWsPayload;
import dk.dtu.scout.backend.websocket.WsReceiver;
import dk.dtu.scout.backend.websocket.WsSender;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RunLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WsReceiver wsReceiver;

    @MockBean
    private WsSender wsSender;

    @Nested
    class StandardRunLifecycle {

        @Test
        void preparedBitstringRun_streamsProgressAndFinishesThroughWebSocket() throws Exception {
            PreparedExecution prepared = prepare(validRunPreparePayload("lifecycle-session-bitstring"));

            wsReceiver.runStart(
                    prepared.executionId(),
                    new StartPreparedExecutionRequest(prepared.sessionId()),
                    headers("ws-lifecycle-bitstring")
            );

            List<RunWsPayload> payloads = captureRunPayloadsAfterFinished(prepared.executionId());

            assertTrue(payloads.stream().anyMatch(wsPayload -> "RUN_PROGRESS".equals(wsPayload.type())));
            assertTrue(payloads.stream().anyMatch(wsPayload -> "RUN_FINISHED".equals(wsPayload.type())));

            RunWsPayload finishedProgress = payloads.stream()
                    .filter(wsPayload -> "RUN_PROGRESS".equals(wsPayload.type()))
                    .filter(wsPayload -> "FINISHED".equals(wsPayload.status()))
                    .findFirst()
                    .orElseThrow();

            assertEquals(prepared.executionId(), finishedProgress.runId());
            assertEquals("bitstring", finishedProgress.searchSpaceId());
            assertEquals("onemax", finishedProgress.problemId());
            assertNotNull(finishedProgress.runtimeMs());
        }

        @Test
        void preparedBitstringRunWithMultipleProblems_executesRealBackendFlow() throws Exception {
            Map<String, Object> payload = validRunPreparePayload("lifecycle-session-multiple-problems");

            @SuppressWarnings("unchecked")
            Map<String, Object> runRequest = (Map<String, Object>) payload.get("runRequest");

            runRequest.put("problemIds", List.of("onemax", "leadingones"));

            PreparedExecution prepared = prepare(payload);

            wsReceiver.runStart(
                    prepared.executionId(),
                    new StartPreparedExecutionRequest(prepared.sessionId()),
                    headers("ws-lifecycle-multiple-problems")
            );

            List<RunWsPayload> payloads = captureRunPayloadsAfterFinished(prepared.executionId());

            assertTrue(payloads.stream()
                    .anyMatch(wsPayload -> "RUN_PROGRESS".equals(wsPayload.type()) && "onemax".equals(wsPayload.problemId())));

            assertTrue(payloads.stream()
                    .anyMatch(wsPayload -> "RUN_PROGRESS".equals(wsPayload.type()) && "leadingones".equals(wsPayload.problemId())));

            assertTrue(payloads.stream().anyMatch(wsPayload -> "RUN_FINISHED".equals(wsPayload.type())));
        }

        @Test
        void preparedBitstringRunWithMultipleRunTimes_executesRealBackendFlow() throws Exception {
            Map<String, Object> payload = validRunPreparePayload("lifecycle-session-multiple-runtimes");

            @SuppressWarnings("unchecked")
            Map<String, Object> runRequest = (Map<String, Object>) payload.get("runRequest");

            runRequest.put("runTimes", 2);

            PreparedExecution prepared = prepare(payload);

            wsReceiver.runStart(
                    prepared.executionId(),
                    new StartPreparedExecutionRequest(prepared.sessionId()),
                    headers("ws-lifecycle-multiple-runtimes")
            );

            List<RunWsPayload> payloads = captureRunPayloadsAfterFinished(prepared.executionId());

            assertTrue(payloads.stream()
                    .anyMatch(wsPayload -> "RUN_PROGRESS".equals(wsPayload.type()) && Integer.valueOf(0).equals(wsPayload.runIndex())));

            assertTrue(payloads.stream()
                    .anyMatch(wsPayload -> "RUN_PROGRESS".equals(wsPayload.type()) && Integer.valueOf(1).equals(wsPayload.runIndex())));

            assertTrue(payloads.stream().anyMatch(wsPayload -> "RUN_FINISHED".equals(wsPayload.type())));
        }

        @Test
        void preparedTspRun_startsThroughWebSocketAndExecutesRealBackendFlow() throws Exception {
            Map<String, Object> payload = validRunPreparePayload("lifecycle-session-tsp");

            @SuppressWarnings("unchecked")
            Map<String, Object> runRequest = (Map<String, Object>) payload.get("runRequest");

            runRequest.put("searchSpaceId", "permutation");
            runRequest.put("searchSpaceParams", Map.of("n", 4));
            runRequest.put("problemIds", List.of("tsp"));
            runRequest.put("problemParams", Map.of("tspInstance", validTspInstancePayload()));
            runRequest.put("generatorId", "2opt");
            runRequest.put("generatorParams", Map.of());
            runRequest.put("parentSelectionRuleId", "random-parents");
            runRequest.put("parentSelectionRuleParams", Map.of());
            runRequest.put("observerIds", List.of("fitness", "tour"));
            runRequest.put("stopConditionParams", Map.of("maxIterations", 3));

            PreparedExecution prepared = prepare(payload);

            wsReceiver.runStart(
                    prepared.executionId(),
                    new StartPreparedExecutionRequest(prepared.sessionId()),
                    headers("ws-lifecycle-tsp")
            );

            List<RunWsPayload> payloads = captureRunPayloadsAfterFinished(prepared.executionId());

            assertTrue(payloads.stream().anyMatch(wsPayload -> "RUN_FINISHED".equals(wsPayload.type())));

            RunWsPayload tspProgress = payloads.stream()
                    .filter(wsPayload -> "RUN_PROGRESS".equals(wsPayload.type()))
                    .filter(wsPayload -> "tsp".equals(wsPayload.problemId()))
                    .filter(wsPayload -> wsPayload.seriesDelta() != null)
                    .filter(wsPayload -> wsPayload.seriesDelta().containsKey("tspTour")
                            || wsPayload.seriesDelta().containsKey("tspCities"))
                    .findFirst()
                    .orElseThrow();

            assertTrue(tspProgress.seriesMerge().containsKey("tspTour")
                    || tspProgress.seriesMerge().containsKey("tspCities"));
        }

        @Test
        void preparedRunWithUnknownGenerator_sendsFailedPayloadWhenStarted() throws Exception {
            Map<String, Object> payload = validRunPreparePayload("lifecycle-session-unknown-generator");

            @SuppressWarnings("unchecked")
            Map<String, Object> runRequest = (Map<String, Object>) payload.get("runRequest");

            runRequest.put("generatorId", "does-not-exist");

            PreparedExecution prepared = prepare(payload);

            wsReceiver.runStart(
                    prepared.executionId(),
                    new StartPreparedExecutionRequest(prepared.sessionId()),
                    headers("ws-lifecycle-unknown-generator")
            );

            List<RunWsPayload> payloads = captureRunPayloadsAfterFailed(prepared.executionId());

            RunWsPayload failedPayload = payloads.stream()
                    .filter(wsPayload -> "RUN_FAILED".equals(wsPayload.type()))
                    .findFirst()
                    .orElseThrow();

            assertTrue(failedPayload.message().contains("Unknown component: does-not-exist"));
        }

        @Test
        void preparedVrpRun_startsThroughWebSocketAndExecutesRealBackendFlow() throws Exception {
            Map<String, Object> payload = validRunPreparePayload("lifecycle-session-vrp");

            @SuppressWarnings("unchecked")
            Map<String, Object> runRequest = (Map<String, Object>) payload.get("runRequest");

            runRequest.put("searchSpaceId", "route-list");
            runRequest.put("searchSpaceParams", Map.of(
                    "n", 2,
                    "numberOfVehicles", 1
            ));
            runRequest.put("problemIds", List.of("vrp"));
            runRequest.put("problemParams", Map.of("vrpInstance", validVrpInstancePayload()));
            runRequest.put("generatorId", "route-list-relocate");
            runRequest.put("generatorParams", Map.of());
            runRequest.put("parentSelectionRuleId", "random-parents");
            runRequest.put("parentSelectionRuleParams", Map.of());
            runRequest.put("observerIds", List.of("fitness"));
            runRequest.put("stopConditionParams", Map.of("maxIterations", 3));

            PreparedExecution prepared = prepare(payload);

            wsReceiver.runStart(
                    prepared.executionId(),
                    new StartPreparedExecutionRequest(prepared.sessionId()),
                    headers("ws-lifecycle-vrp")
            );

            List<RunWsPayload> payloads = captureRunPayloadsAfterFinished(prepared.executionId());

            String debugPayloads = payloads.stream()
                    .map(wsPayload -> "type=" + wsPayload.type()
                            + ", status=" + wsPayload.status()
                            + ", problemId=" + wsPayload.problemId()
                            + ", message=" + wsPayload.message())
                    .toList()
                    .toString();

            assertTrue(
                    payloads.stream().anyMatch(wsPayload ->
                            "RUN_PROGRESS".equals(wsPayload.type()) && "vrp".equals(wsPayload.problemId())),
                    debugPayloads
            );

            assertTrue(
                    payloads.stream().anyMatch(wsPayload -> "RUN_FINISHED".equals(wsPayload.type())),
                    debugPayloads
            );
        }
    }

    @Nested
    class RuntimeStudyLifecycle {

        @Test
        void preparedRuntimeStudy_startsThroughWebSocketAndExecutesRealBackendFlow() throws Exception {
            PreparedExecution prepared = prepare(validRuntimeStudyPreparePayload("lifecycle-session-study"));

            wsReceiver.studyStart(
                    prepared.executionId(),
                    new StartPreparedExecutionRequest(prepared.sessionId()),
                    headers("ws-lifecycle-study")
            );

            List<RuntimeStudyWsPayload> payloads = captureStudyPayloadsAfterFinished(prepared.executionId());

            assertTrue(payloads.stream().anyMatch(wsPayload -> "STUDY_PROGRESS".equals(wsPayload.type())));
            assertTrue(payloads.stream().anyMatch(wsPayload -> "STUDY_FINISHED".equals(wsPayload.type())));
        }
    }

    private List<RunWsPayload> captureRunPayloads(String runId) {
        ArgumentCaptor<RunWsPayload> captor = ArgumentCaptor.forClass(RunWsPayload.class);

        verify(wsSender, timeout(5000).atLeastOnce())
                .sendToRun(eq(runId), captor.capture());

        return captor.getAllValues();
    }

    private List<RunWsPayload> captureRunPayloadsAfterFinished(String runId) {
        verify(wsSender, timeout(5000).atLeastOnce())
                .sendToRun(eq(runId), argThat(argument ->
                        argument instanceof RunWsPayload wsPayload
                                && "RUN_FINISHED".equals(wsPayload.type())
                ));

        return captureRunPayloads(runId);
    }

    private List<RunWsPayload> captureRunPayloadsAfterFailed(String runId) {
        verify(wsSender, timeout(5000).atLeastOnce())
                .sendToRun(eq(runId), argThat(argument ->
                        argument instanceof RunWsPayload wsPayload
                                && "RUN_FAILED".equals(wsPayload.type())
                ));

        return captureRunPayloads(runId);
    }

    private List<RuntimeStudyWsPayload> captureStudyPayloads(String studyId) {
        ArgumentCaptor<RuntimeStudyWsPayload> captor = ArgumentCaptor.forClass(RuntimeStudyWsPayload.class);

        verify(wsSender, timeout(5000).atLeastOnce())
                .sendToStudy(eq(studyId), captor.capture());

        return captor.getAllValues();
    }

    private List<RuntimeStudyWsPayload> captureStudyPayloadsAfterFinished(String studyId) {
        verify(wsSender, timeout(5000).atLeastOnce())
                .sendToStudy(eq(studyId), argThat(argument ->
                        argument instanceof RuntimeStudyWsPayload wsPayload
                                && "STUDY_FINISHED".equals(wsPayload.type())
                ));

        return captureStudyPayloads(studyId);
    }

    private PreparedExecution prepare(Map<String, Object> payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/run/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                Map.class
        );

        return new PreparedExecution(
                response.get("sessionId").toString(),
                response.get("executionId").toString()
        );
    }

    private static SimpMessageHeaderAccessor headers(String websocketSessionId) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
        headers.setSessionId(websocketSessionId);
        return headers;
    }

    private record PreparedExecution(String sessionId, String executionId) {
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
        request.put("stopConditionIds", List.of("max-iterations"));
        request.put("stopConditionParams", Map.of("maxIterations", 5));
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
        request.put("problemSizes", List.of(2));
        request.put("repetitionsPerSize", 1);
        return request;
    }

    private static Map<String, Object> validTspInstancePayload() {
        Map<String, Object> tsp = new LinkedHashMap<>();
        tsp.put("name", "tiny");
        tsp.put("comment", "");
        tsp.put("cities", List.of(
                Map.of("x", 0.0, "y", 0.0),
                Map.of("x", 1.0, "y", 0.0),
                Map.of("x", 1.0, "y", 1.0),
                Map.of("x", 0.0, "y", 1.0)
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
                Map.of("x", 1.0, "y", 1.0, "demand", 1.0),
                Map.of("x", 2.0, "y", 2.0, "demand", 1.0)
        ));
        return vrp;
    }
}