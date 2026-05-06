package dk.dtu.scout.backend.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dtu.scout.backend.dto.request.StartPreparedExecutionRequest;
import dk.dtu.scout.backend.dto.ws.RunWsPayload;
import dk.dtu.scout.backend.dto.ws.RuntimeStudyWsPayload;
import dk.dtu.scout.backend.websocket.WsReceiver;
import dk.dtu.scout.backend.websocket.WsSender;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WsReceiverErrorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WsReceiver wsReceiver;

    @MockBean
    private WsSender wsSender;

    @Nested
    class RunStartErrors {

        @Test
        void runStart_sendsFailedWhenPreparedRunDoesNotExist() {
            wsReceiver.runStart(
                "missing-run-id",
                new StartPreparedExecutionRequest("session-missing-run"),
                headers("ws-missing-run")
            );

            verify(wsSender, timeout(2000))
                .sendToRun(eq("missing-run-id"), any(RunWsPayload.class));
        }

        @Test
        void runStart_sendsFailedWhenSessionDoesNotMatchPreparedRunOwner() throws Exception {
            PreparedExecution prepared = prepare(validRunPreparePayload("owner-session-run"));

            wsReceiver.runStart(
                prepared.executionId(),
                new StartPreparedExecutionRequest("other-session"),
                headers("ws-wrong-run-session")
            );

            verify(wsSender, timeout(2000))
                .sendToRun(eq(prepared.executionId()), any(RunWsPayload.class));
        }

        @Test
        void runStart_sendsFailedWhenStartRequestIsNull() {
            wsReceiver.runStart(
                "run-null-request",
                null,
                headers("ws-null-run-request")
            );

            verify(wsSender, timeout(2000))
                .sendToRun(eq("run-null-request"), any(RunWsPayload.class));
        }
    }

    @Nested
    class StudyStartErrors {

        @Test
        void studyStart_sendsFailedWhenPreparedStudyDoesNotExist() {
            wsReceiver.studyStart(
                "missing-study-id",
                new StartPreparedExecutionRequest("session-missing-study"),
                headers("ws-missing-study")
            );

            verify(wsSender, timeout(2000))
                .sendToStudy(eq("missing-study-id"), any(RuntimeStudyWsPayload.class));
        }

        @Test
        void studyStart_sendsFailedWhenSessionDoesNotMatchPreparedStudyOwner() throws Exception {
            PreparedExecution prepared = prepare(validRuntimeStudyPreparePayload("owner-session-study"));

            wsReceiver.studyStart(
                prepared.executionId(),
                new StartPreparedExecutionRequest("other-session"),
                headers("ws-wrong-study-session")
            );

            verify(wsSender, timeout(2000))
                .sendToStudy(eq(prepared.executionId()), any(RuntimeStudyWsPayload.class));
        }

        @Test
        void studyStart_sendsFailedWhenStartRequestIsNull() {
            wsReceiver.studyStart(
                "study-null-request",
                null,
                headers("ws-null-study-request")
            );

            verify(wsSender, timeout(2000))
                .sendToStudy(eq("study-null-request"), any(RuntimeStudyWsPayload.class));
        }
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
}