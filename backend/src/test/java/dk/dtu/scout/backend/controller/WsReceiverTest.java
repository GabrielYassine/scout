package dk.dtu.scout.backend.controller;

import dk.dtu.scout.backend.dto.request.RunRequest;
import dk.dtu.scout.backend.dto.request.RuntimeStudyRequest;
import dk.dtu.scout.backend.dto.request.StartPreparedExecutionRequest;
import dk.dtu.scout.backend.dto.ws.RunWsPayload;
import dk.dtu.scout.backend.dto.ws.RuntimeStudyWsPayload;
import dk.dtu.scout.backend.service.ExecutionRegistry;
import dk.dtu.scout.backend.service.RunOrchestratorService;
import dk.dtu.scout.backend.websocket.WsReceiver;
import dk.dtu.scout.backend.websocket.WsSender;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest
class WsReceiverTest {

    @Autowired
    private WsReceiver wsReceiver;

    @Autowired
    private ExecutionRegistry executionRegistry;

    @MockBean
    private RunOrchestratorService runOrchestratorService;

    @MockBean
    private WsSender wsSender;

    @Nested
    class RunStart {

        @Test
        void runStart_consumesPreparedRunAndStartsIt() {
            RunRequest preparedRun = validRunRequest("session-run-start", "run-start-id");
            executionRegistry.storePreparedRun(preparedRun);

            wsReceiver.runStart("run-start-id", new StartPreparedExecutionRequest("session-run-start"), headers("ws-run-start"));

            ArgumentCaptor<RunRequest> captor = ArgumentCaptor.forClass(RunRequest.class);
            verify(runOrchestratorService).startRun(captor.capture());
            verifyNoInteractions(wsSender);

            assertEquals("run-start-id", captor.getValue().runId());
            assertEquals("session-run-start", captor.getValue().sessionId());
        }

        @Test
        void runStart_sendsFailedWhenPreparedRunDoesNotExist() {
            wsReceiver.runStart("missing-run-id", new StartPreparedExecutionRequest("session-missing-run"), headers("ws-missing-run"));

            verify(runOrchestratorService, never()).startRun(any());
            verify(wsSender).sendToRun(eq("missing-run-id"), any(RunWsPayload.class));
        }

        @Test
        void runStart_sendsFailedWhenSessionDoesNotMatch() {
            executionRegistry.storePreparedRun(validRunRequest("owner-session", "run-wrong-session"));

            wsReceiver.runStart("run-wrong-session", new StartPreparedExecutionRequest("other-session"), headers("ws-wrong-session"));

            verify(runOrchestratorService, never()).startRun(any());
            verify(wsSender).sendToRun(eq("run-wrong-session"), any(RunWsPayload.class));
        }

        @Test
        void runStart_sendsFailedWhenStartRequestIsNull() {
            wsReceiver.runStart("run-null-request", null, headers("ws-null-run-request"));

            verify(runOrchestratorService, never()).startRun(any());
            verify(wsSender).sendToRun(eq("run-null-request"), any(RunWsPayload.class));
        }
    }

    @Nested
    class StudyStart {

        @Test
        void studyStart_consumesPreparedStudyAndStartsIt() {
            RuntimeStudyRequest preparedStudy = validRuntimeStudyRequest("session-study-start", "study-start-id");
            executionRegistry.storePreparedStudy(preparedStudy);

            wsReceiver.studyStart("study-start-id", new StartPreparedExecutionRequest("session-study-start"), headers("ws-study-start"));

            ArgumentCaptor<RuntimeStudyRequest> captor = ArgumentCaptor.forClass(RuntimeStudyRequest.class);
            verify(runOrchestratorService).startRuntimeStudy(captor.capture());
            verifyNoInteractions(wsSender);

            assertEquals("study-start-id", captor.getValue().studyId());
            assertEquals("session-study-start", captor.getValue().sessionId());
        }

        @Test
        void studyStart_sendsFailedWhenPreparedStudyDoesNotExist() {
            wsReceiver.studyStart("missing-study-id", new StartPreparedExecutionRequest("session-missing-study"), headers("ws-missing-study"));

            verify(runOrchestratorService, never()).startRuntimeStudy(any());
            verify(wsSender).sendToStudy(eq("missing-study-id"), any(RuntimeStudyWsPayload.class));
        }

        @Test
        void studyStart_sendsFailedWhenSessionDoesNotMatch() {
            executionRegistry.storePreparedStudy(validRuntimeStudyRequest("owner-session", "study-wrong-session"));

            wsReceiver.studyStart("study-wrong-session", new StartPreparedExecutionRequest("other-session"), headers("ws-wrong-study-session"));

            verify(runOrchestratorService, never()).startRuntimeStudy(any());
            verify(wsSender).sendToStudy(eq("study-wrong-session"), any(RuntimeStudyWsPayload.class));
        }

        @Test
        void studyStart_sendsFailedWhenStartRequestIsNull() {
            wsReceiver.studyStart("study-null-request", null, headers("ws-null-study-request"));

            verify(runOrchestratorService, never()).startRuntimeStudy(any());
            verify(wsSender).sendToStudy(eq("study-null-request"), any(RuntimeStudyWsPayload.class));
        }
    }

    private static SimpMessageHeaderAccessor headers(String websocketSessionId) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
        headers.setSessionId(websocketSessionId);
        return headers;
    }

    private static RunRequest validRunRequest(String sessionId, String runId) {
        return new RunRequest(
            "bitstring",
            Map.of("n", 10),
            List.of("onemax"),
            Map.of(),
            "bit-flip",
            Map.of("flipProbability", "1/n"),
            "mu-lambda",
            Map.of("mu", 1, "lambda", 1),
            "mu-plus-lambda",
            Map.of(),
            "elitist-parents",
            Map.of(),
            null,
            null,
            List.of("fitness"),
            Map.of(),
            List.of("max-iterations"),
            Map.of("maxIterations", 10),
            1234L,
            1,
            sessionId,
            runId,
            1,
            1
        );
    }

    private static RuntimeStudyRequest validRuntimeStudyRequest(String sessionId, String studyId) {
        return new RuntimeStudyRequest(
            studyId,
            sessionId,
            "bitstring",
            Map.of("n", 10),
            "onemax",
            Map.of(),
            "bit-flip",
            Map.of("flipProbability", "1/n"),
            "mu-plus-lambda",
            Map.of(),
            "mu-lambda",
            Map.of("mu", 1, "lambda", 1),
            "elitist-parents",
            Map.of(),
            null,
            null,
            List.of("optimum-reached"),
            Map.of(),
            1234L,
            List.of(5, 10),
            1
        );
    }
}