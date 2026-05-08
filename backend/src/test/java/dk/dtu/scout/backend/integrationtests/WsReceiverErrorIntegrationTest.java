package dk.dtu.scout.backend.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dtu.scout.backend.dto.request.PrepareRunResponse;
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
import org.springframework.test.web.servlet.MockMvc;

import static dk.dtu.scout.backend.integrationtests.support.RunPrepareTestSupport.prepareRun;
import static dk.dtu.scout.backend.integrationtests.support.RunPrepareTestSupport.prepareRuntimeStudy;
import static dk.dtu.scout.backend.integrationtests.support.RunRequestFixtures.validRunPreparePayload;
import static dk.dtu.scout.backend.integrationtests.support.RunRequestFixtures.validRuntimeStudyPreparePayload;
import static dk.dtu.scout.backend.integrationtests.support.WebSocketTestSupport.headers;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

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
            startRunAndVerifyFailure(
                "missing-run-id",
                "session-missing-run",
                "ws-missing-run"
            );
        }

        @Test
        void runStart_sendsFailedWhenSessionDoesNotMatchPreparedRunOwner() throws Exception {
            PrepareRunResponse prepared = prepareRun(
                mockMvc,
                objectMapper,
                validRunPreparePayload("owner-session-run")
            );

            startRunAndVerifyFailure(
                prepared.executionId(),
                "other-session",
                "ws-wrong-run-session"
            );
        }

        @Test
        void runStart_sendsFailedWhenStartRequestIsNull() {
            wsReceiver.runStart(
                "run-null-request",
                null,
                headers("ws-null-run-request")
            );

            verifyRunFailureSent("run-null-request");
        }
    }

    @Nested
    class StudyStartErrors {

        @Test
        void studyStart_sendsFailedWhenPreparedStudyDoesNotExist() {
            startStudyAndVerifyFailure(
                "missing-study-id",
                "session-missing-study",
                "ws-missing-study"
            );
        }

        @Test
        void studyStart_sendsFailedWhenSessionDoesNotMatchPreparedStudyOwner() throws Exception {
            PrepareRunResponse  prepared = prepareRuntimeStudy(
                mockMvc,
                objectMapper,
                validRuntimeStudyPreparePayload("owner-session-study")
            );

            startStudyAndVerifyFailure(
                prepared.executionId(),
                "other-session",
                "ws-wrong-study-session"
            );
        }

        @Test
        void studyStart_sendsFailedWhenStartRequestIsNull() {
            wsReceiver.studyStart(
                "study-null-request",
                null,
                headers("ws-null-study-request")
            );

            verifyStudyFailureSent("study-null-request");
        }
    }

    private void startRunAndVerifyFailure(String runId, String sessionId, String websocketSessionId) {
        wsReceiver.runStart(
            runId,
            new StartPreparedExecutionRequest(sessionId),
            headers(websocketSessionId)
        );

        verifyRunFailureSent(runId);
    }

    private void startStudyAndVerifyFailure(String studyId, String sessionId, String websocketSessionId) {
        wsReceiver.studyStart(
            studyId,
            new StartPreparedExecutionRequest(sessionId),
            headers(websocketSessionId)
        );

        verifyStudyFailureSent(studyId);
    }

    private void verifyRunFailureSent(String runId) {
        ArgumentCaptor<RunWsPayload> captor = ArgumentCaptor.forClass(RunWsPayload.class);

        verify(wsSender, timeout(2000)).sendToRun(eq(runId), captor.capture());

        RunWsPayload payload = captor.getValue();

        assertNotNull(payload);
        assertEquals("RUN_FAILED", payload.type());
    }

    private void verifyStudyFailureSent(String studyId) {
        ArgumentCaptor<RuntimeStudyWsPayload> captor = ArgumentCaptor.forClass(RuntimeStudyWsPayload.class);

        verify(wsSender, timeout(2000)).sendToStudy(eq(studyId), captor.capture());

        RuntimeStudyWsPayload payload = captor.getValue();

        assertNotNull(payload);
        assertEquals("STUDY_FAILED", payload.type());
    }
}