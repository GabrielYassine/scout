package dk.dtu.scout.backend.integrationtests.support;

import dk.dtu.scout.backend.dto.request.StartPreparedExecutionRequest;
import dk.dtu.scout.backend.dto.ws.RunWsPayload;
import dk.dtu.scout.backend.dto.ws.RuntimeStudyWsPayload;
import dk.dtu.scout.backend.websocket.WsReceiver;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public final class WebSocketTestSupport {

    private WebSocketTestSupport() {
    }

    public static SimpMessageHeaderAccessor headers(String websocketSessionId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        accessor.setSessionId(websocketSessionId);
        return accessor;
    }

    public static void startPreparedRun(
        WsReceiver wsReceiver,
        PreparedExecution prepared,
        String websocketSessionId
    ) {
        wsReceiver.runStart(
            prepared.executionId(),
            new StartPreparedExecutionRequest(prepared.sessionId()),
            headers(websocketSessionId)
        );
    }

    public static void startPreparedStudy(
        WsReceiver wsReceiver,
        PreparedExecution prepared,
        String websocketSessionId
    ) {
        wsReceiver.studyStart(
            prepared.executionId(),
            new StartPreparedExecutionRequest(prepared.sessionId()),
            headers(websocketSessionId)
        );
    }

    public static <T> List<T> capturePayloads(
        Object mockSender,
        PayloadVerifier<T> verifier
    ) {
        ArgumentCaptor<T> captor = verifier.capture(mockSender);
        return captor.getAllValues();
    }

    public static List<RunWsPayload> captureRunPayloads(Object mockSender, String runId) {
        ArgumentCaptor<RunWsPayload> captor = ArgumentCaptor.forClass(RunWsPayload.class);
        verify((dk.dtu.scout.backend.websocket.WsSender) mockSender, timeout(5000).atLeastOnce())
            .sendToRun(eq(runId), captor.capture());
        return captor.getAllValues();
    }

    public static List<RunWsPayload> captureRunPayloadsAfterFinished(Object mockSender, String runId) {
        verify((dk.dtu.scout.backend.websocket.WsSender) mockSender, timeout(5000).atLeastOnce())
            .sendToRun(eq(runId), argThat(argument ->
                argument instanceof RunWsPayload wsPayload && "RUN_FINISHED".equals(wsPayload.type())
            ));
        return captureRunPayloads(mockSender, runId);
    }

    public static List<RunWsPayload> captureRunPayloadsAfterFailed(Object mockSender, String runId) {
        verify((dk.dtu.scout.backend.websocket.WsSender) mockSender, timeout(5000).atLeastOnce())
            .sendToRun(eq(runId), argThat(argument ->
                argument instanceof RunWsPayload wsPayload && "RUN_FAILED".equals(wsPayload.type())
            ));
        return captureRunPayloads(mockSender, runId);
    }

    public static List<RuntimeStudyWsPayload> captureStudyPayloads(Object mockSender, String studyId) {
        ArgumentCaptor<RuntimeStudyWsPayload> captor = ArgumentCaptor.forClass(RuntimeStudyWsPayload.class);
        verify((dk.dtu.scout.backend.websocket.WsSender) mockSender, timeout(5000).atLeastOnce())
            .sendToStudy(eq(studyId), captor.capture());
        return captor.getAllValues();
    }

    public static List<RuntimeStudyWsPayload> captureStudyPayloadsAfterFinished(Object mockSender, String studyId) {
        verify((dk.dtu.scout.backend.websocket.WsSender) mockSender, timeout(5000).atLeastOnce())
            .sendToStudy(eq(studyId), argThat(argument ->
                argument instanceof RuntimeStudyWsPayload wsPayload && "STUDY_FINISHED".equals(wsPayload.type())
            ));
        return captureStudyPayloads(mockSender, studyId);
    }

    public static void assertRunFinished(List<RunWsPayload> payloads, String runId) {
        assertTrue(payloads.stream().anyMatch(payload -> "RUN_PROGRESS".equals(payload.type())));
        assertTrue(payloads.stream().anyMatch(payload -> "RUN_FINISHED".equals(payload.type())));

        RunWsPayload finishedProgress = payloads.stream()
            .filter(payload -> "RUN_PROGRESS".equals(payload.type()))
            .filter(payload -> "FINISHED".equals(payload.status()))
            .findFirst()
            .orElseThrow();

        assertEquals(runId, finishedProgress.runId());
        assertNotNull(finishedProgress.runtimeMs());
    }

    public static void assertStudyFinished(List<RuntimeStudyWsPayload> payloads, String studyId) {
        assertTrue(payloads.stream().anyMatch(payload -> "STUDY_PROGRESS".equals(payload.type())));
        assertTrue(payloads.stream().anyMatch(payload -> "STUDY_FINISHED".equals(payload.type())));

        RuntimeStudyWsPayload finishedPayload = payloads.stream()
            .filter(payload -> "STUDY_FINISHED".equals(payload.type()))
            .findFirst()
            .orElseThrow();

        assertEquals(studyId, finishedPayload.studyId());
    }

    @FunctionalInterface
    public interface PayloadVerifier<T> {
        ArgumentCaptor<T> capture(Object mockSender);
    }
}