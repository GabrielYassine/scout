package dk.dtu.scout.backend.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dtu.scout.backend.dto.request.RunRequest;
import dk.dtu.scout.backend.dto.request.StartPreparedExecutionRequest;
import dk.dtu.scout.backend.dto.run.RunGroupResponse;
import dk.dtu.scout.backend.dto.run.RuntimeStudyPointResponse;
import dk.dtu.scout.backend.dto.ws.RunWsPayload;
import dk.dtu.scout.backend.dto.ws.RuntimeStudyWsPayload;
import dk.dtu.scout.backend.integrationtests.support.PreparedExecution;
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
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static dk.dtu.scout.backend.integrationtests.support.RunPrepareTestSupport.prepareRun;
import static dk.dtu.scout.backend.integrationtests.support.RunPrepareTestSupport.prepareRuntimeStudy;
import static dk.dtu.scout.backend.integrationtests.support.RunRequestFixtures.validRunPreparePayload;
import static dk.dtu.scout.backend.integrationtests.support.RunRequestFixtures.validRuntimeStudyPreparePayload;
import static dk.dtu.scout.backend.integrationtests.support.WebSocketTestSupport.headers;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
            PreparedExecution prepared = prepareRunPayload("disconnect-run-session");

            startRunWithWrongSession(prepared);
            disconnect("ws-disconnect-prepared-run");

            wsReceiver.runStart(
                prepared.executionId(),
                new StartPreparedExecutionRequest(prepared.sessionId()),
                headers("ws-disconnect-prepared-run-retry")
            );

            verify(wsSender, timeout(2000).atLeastOnce()).sendToRun(eq(prepared.executionId()), any(RunWsPayload.class));
        }
    }

    @Nested
    class PreparedStudyDisconnect {

        @Test
        void disconnectAfterStudyWebSocketAttach_removesPreparedStudy() throws Exception {
            PreparedExecution prepared = prepareStudyPayload("disconnect-study-session");

            startStudyWithWrongSession(prepared);
            disconnect("ws-disconnect-prepared-study");

            wsReceiver.studyStart(
                prepared.executionId(),
                new StartPreparedExecutionRequest(prepared.sessionId()),
                headers("ws-disconnect-prepared-study-retry")
            );

            verify(wsSender, timeout(2000).atLeastOnce()).sendToStudy(eq(prepared.executionId()), any(RuntimeStudyWsPayload.class));
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
            PreparedExecution prepared = prepareRunPayload("disconnect-active-run-session");

            CountDownLatch runStarted = new CountDownLatch(1);
            CountDownLatch runInterrupted = new CountDownLatch(1);

            mockInterruptedRun(runStarted, runInterrupted, "run cancelled by disconnect");

            wsReceiver.runStart(
                prepared.executionId(),
                new StartPreparedExecutionRequest(prepared.sessionId()),
                headers("ws-active-run-disconnect")
            );

            assertTrue(runStarted.await(2, TimeUnit.SECONDS));

            disconnect("ws-active-run-disconnect");

            assertTrue(runInterrupted.await(2, TimeUnit.SECONDS));
        }
    }

    @Nested
    class ActiveStudyDisconnect {

        @Test
        void disconnectDuringActiveRuntimeStudy_cancelsActiveStudy() throws Exception {
            PreparedExecution prepared = prepareStudyPayload("disconnect-active-study-session");

            CountDownLatch studyStarted = new CountDownLatch(1);
            CountDownLatch studyInterrupted = new CountDownLatch(1);

            mockInterruptedRun(studyStarted, studyInterrupted, "study cancelled by disconnect");

            when(runStatisticsService.toRuntimeStudyPoint(anyInt(), any()))
                .thenReturn(new RuntimeStudyPointResponse(2, 0.0, List.of()));

            wsReceiver.studyStart(
                prepared.executionId(),
                new StartPreparedExecutionRequest(prepared.sessionId()),
                headers("ws-active-study-disconnect")
            );

            assertTrue(studyStarted.await(2, TimeUnit.SECONDS));

            disconnect("ws-active-study-disconnect");

            assertTrue(studyInterrupted.await(2, TimeUnit.SECONDS));
        }
    }

    private PreparedExecution prepareRunPayload(String sessionId) throws Exception {
        return prepareRun(mockMvc, objectMapper, validRunPreparePayload(sessionId));
    }

    private PreparedExecution prepareStudyPayload(String sessionId) throws Exception {
        return prepareRuntimeStudy(mockMvc, objectMapper, validRuntimeStudyPreparePayload(sessionId));
    }

    private void startRunWithWrongSession(PreparedExecution prepared) {
        wsReceiver.runStart(
            prepared.executionId(),
            new StartPreparedExecutionRequest("wrong-session"),
            headers("ws-disconnect-prepared-run")
        );
    }

    private void startStudyWithWrongSession(PreparedExecution prepared) {
        wsReceiver.studyStart(
            prepared.executionId(),
            new StartPreparedExecutionRequest("wrong-session"),
            headers("ws-disconnect-prepared-study")
        );
    }

    private void mockInterruptedRun(
        CountDownLatch started,
        CountDownLatch interrupted,
        String cancellationMessage
    ) {
        when(runExecutor.runBatch(any(RunRequest.class), anyInt(), anyInt()))
            .thenAnswer(invocation -> {
                started.countDown();

                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                    interrupted.countDown();
                    Thread.currentThread().interrupt();
                    throw new java.util.concurrent.CancellationException(cancellationMessage);
                }

                return List.<RunGroupResponse>of();
            });
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
}