package dk.dtu.scout.backend.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dtu.scout.backend.dto.request.RunRequest;
import dk.dtu.scout.backend.dto.request.StartPreparedExecutionRequest;
import dk.dtu.scout.backend.dto.run.RunGroupResponse;
import dk.dtu.scout.backend.dto.run.RuntimeStudyPointResponse;
import dk.dtu.scout.backend.dto.ws.RunWsPayload;
import dk.dtu.scout.backend.dto.ws.RuntimeStudyWsPayload;
import dk.dtu.scout.backend.service.RunExecutor;
import dk.dtu.scout.backend.service.RunStatisticsService;
import dk.dtu.scout.backend.websocket.WebSocketDisconnectListener;
import dk.dtu.scout.backend.websocket.WsReceiver;
import dk.dtu.scout.backend.websocket.WsSender;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for websocket disconnect handling.
 * Covers cleanup of prepared executions and cancellation of active executions.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WebSocketDisconnectLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WsReceiver wsReceiver;

    @Autowired
    private WebSocketDisconnectListener disconnectListener;

    @MockBean
    private WsSender wsSender;

    @MockBean
    private RunExecutor runExecutor;

    @MockBean
    private RunStatisticsService runStatisticsService;

    @Nested
    class PreparedRunDisconnect {

        @Test
        void disconnectAfterRunWebSocketAttach_removesPreparedRun() throws Exception {
            PreparedExecution prepared = prepare(validRunPreparePayload("disconnect-run-session"));

            String websocketSessionId = "ws-disconnect-prepared-run";

            wsReceiver.runStart(
                    prepared.executionId(),
                    new StartPreparedExecutionRequest("wrong-session"),
                    headers(websocketSessionId)
            );

            disconnect(websocketSessionId);

            wsReceiver.runStart(
                    prepared.executionId(),
                    new StartPreparedExecutionRequest(prepared.sessionId()),
                    headers("ws-disconnect-prepared-run-retry")
            );

            verify(wsSender, timeout(2000).atLeastOnce())
                    .sendToRun(eq(prepared.executionId()), any(RunWsPayload.class));
        }
    }

    @Nested
    class PreparedStudyDisconnect {

        @Test
        void disconnectAfterStudyWebSocketAttach_removesPreparedStudy() throws Exception {
            PreparedExecution prepared = prepare(validRuntimeStudyPreparePayload("disconnect-study-session"));

            String websocketSessionId = "ws-disconnect-prepared-study";

            wsReceiver.studyStart(
                    prepared.executionId(),
                    new StartPreparedExecutionRequest("wrong-session"),
                    headers(websocketSessionId)
            );

            disconnect(websocketSessionId);

            wsReceiver.studyStart(
                    prepared.executionId(),
                    new StartPreparedExecutionRequest(prepared.sessionId()),
                    headers("ws-disconnect-prepared-study-retry")
            );

            verify(wsSender, timeout(2000).atLeastOnce())
                    .sendToStudy(eq(prepared.executionId()), any(RuntimeStudyWsPayload.class));
        }
    }

    @Nested
    class UnknownDisconnect {

        @Test
        void disconnectWithoutRegisteredWebSocketBinding_isIgnored() {
            assertDoesNotThrow(() -> disconnect("ws-session-never-attached"));
        }
    }

    @Nested
    class ActiveRunDisconnect {

        @Test
        void disconnectDuringActiveRun_cancelsActiveRun() throws Exception {
            PreparedExecution prepared = prepare(validRunPreparePayload("disconnect-active-run-session"));

            CountDownLatch runStarted = new CountDownLatch(1);
            CountDownLatch runInterrupted = new CountDownLatch(1);

            when(runExecutor.runBatch(any(RunRequest.class), anyInt(), anyInt())).thenAnswer(invocation -> {
                runStarted.countDown();

                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                    runInterrupted.countDown();
                    Thread.currentThread().interrupt();
                    throw new java.util.concurrent.CancellationException("run cancelled by disconnect");
                }

                return List.<RunGroupResponse>of();
            });

            String websocketSessionId = "ws-active-run-disconnect";

            wsReceiver.runStart(
                    prepared.executionId(),
                    new StartPreparedExecutionRequest(prepared.sessionId()),
                    headers(websocketSessionId)
            );

            assertTrue(runStarted.await(2, TimeUnit.SECONDS));

            disconnect(websocketSessionId);

            assertTrue(runInterrupted.await(2, TimeUnit.SECONDS));
        }
    }

    @Nested
    class ActiveStudyDisconnect {

        @Test
        void disconnectDuringActiveRuntimeStudy_cancelsActiveStudy() throws Exception {
            PreparedExecution prepared = prepare(validRuntimeStudyPreparePayload("disconnect-active-study-session"));

            CountDownLatch studyStarted = new CountDownLatch(1);
            CountDownLatch studyInterrupted = new CountDownLatch(1);

            when(runExecutor.runBatch(any(RunRequest.class), anyInt(), anyInt())).thenAnswer(invocation -> {
                studyStarted.countDown();

                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                    studyInterrupted.countDown();
                    Thread.currentThread().interrupt();
                    throw new java.util.concurrent.CancellationException("study cancelled by disconnect");
                }

                return List.<RunGroupResponse>of();
            });

            when(runStatisticsService.toRuntimeStudyPoint(anyInt(), any()))
                    .thenReturn(new RuntimeStudyPointResponse(2, 0.0, List.of()));

            String websocketSessionId = "ws-active-study-disconnect";

            wsReceiver.studyStart(
                    prepared.executionId(),
                    new StartPreparedExecutionRequest(prepared.sessionId()),
                    headers(websocketSessionId)
            );

            assertTrue(studyStarted.await(2, TimeUnit.SECONDS));

            disconnect(websocketSessionId);

            assertTrue(studyInterrupted.await(2, TimeUnit.SECONDS));
        }
    }

    private void disconnect(String websocketSessionId) {
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();

        SessionDisconnectEvent event = new SessionDisconnectEvent(
                this,
                message,
                websocketSessionId,
                CloseStatus.NORMAL
        );

        disconnectListener.handleDisconnect(event);
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
        request.put("stopConditionIds", List.of("max-evaluations"));
        request.put("stopConditionParams", Map.of("maxEvaluations", 5));
        request.put("seed", 1234L);
        request.put("runTimes", 1);
        request.put("sessionId", null);
        request.put("runId", null);
        request.put("logEveryEvaluations", 1);
        request.put("wsUpdateEveryEvaluations", 1);
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
}