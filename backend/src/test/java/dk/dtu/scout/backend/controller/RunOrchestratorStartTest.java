package dk.dtu.scout.backend.controller;

import dk.dtu.scout.backend.dto.request.RunRequest;
import dk.dtu.scout.backend.dto.request.RuntimeStudyRequest;
import dk.dtu.scout.backend.dto.run.BatchSummaryResponse;
import dk.dtu.scout.backend.dto.run.RunGroupResponse;
import dk.dtu.scout.backend.dto.run.RuntimeStudyPointResponse;
import dk.dtu.scout.backend.dto.ws.RunWsPayload;
import dk.dtu.scout.backend.dto.ws.RuntimeStudyWsPayload;
import dk.dtu.scout.backend.service.RunExecutor;
import dk.dtu.scout.backend.service.RunOrchestratorService;
import dk.dtu.scout.backend.service.RunStatisticsService;
import dk.dtu.scout.backend.websocket.WsSender;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
class RunOrchestratorStartTest {

    @Autowired
    private RunOrchestratorService runOrchestratorService;

    @MockBean
    private RunExecutor runExecutor;

    @MockBean
    private RunStatisticsService runStatisticsService;

    @MockBean
    private WsSender wsSender;

    @Nested
    class StandardRunStart {

        @Test
        void startRun_executesRunAndSendsFinishedPayload() {
            RunRequest request = validRunRequest("start-session", "start-run");

            BatchSummaryResponse summary = mock(BatchSummaryResponse.class);

            when(runExecutor.runBatch(eq(request), eq(1), eq(1))).thenReturn(List.<RunGroupResponse>of());
            when(runStatisticsService.calculateSummary(List.of())).thenReturn(summary);

            runOrchestratorService.startRun(request);

            verify(runExecutor, timeout(2000)).runBatch(eq(request), eq(1), eq(1));
            verify(runStatisticsService, timeout(2000)).calculateSummary(List.of());
            verify(wsSender, timeout(2000)).sendToRun(eq("start-run"), any(RunWsPayload.class));
        }

        @Test
        void startRun_usesLogEveryAsWebSocketCadenceWhenWsUpdateEveryIsMissing() {
            RunRequest request = validRunRequest("start-session-default-ws", "start-run-default-ws", 5, 0);

            BatchSummaryResponse summary = mock(BatchSummaryResponse.class);

            when(runExecutor.runBatch(eq(request), eq(5), eq(5))).thenReturn(List.<RunGroupResponse>of());
            when(runStatisticsService.calculateSummary(List.of())).thenReturn(summary);

            runOrchestratorService.startRun(request);

            verify(runExecutor, timeout(2000)).runBatch(eq(request), eq(5), eq(5));
            verify(wsSender, timeout(2000)).sendToRun(eq("start-run-default-ws"), any(RunWsPayload.class));
        }

        @Test
        void startRun_sendsFailedPayloadWhenRunFails() {
            RunRequest request = validRunRequest("failed-session", "failed-run");

            when(runExecutor.runBatch(eq(request), eq(1), eq(1))).thenThrow(new RuntimeException("boom"));

            runOrchestratorService.startRun(request);

            verify(runExecutor, timeout(2000)).runBatch(eq(request), eq(1), eq(1));
            verify(runStatisticsService, after(500).never()).calculateSummary(any());
            verify(wsSender, timeout(2000)).sendToRun(eq("failed-run"), any(RunWsPayload.class));
        }
    }

    @Nested
    class RuntimeStudyStart {

        @Test
        void startRuntimeStudy_executesStudyAndSendsProgressAndFinishedPayloads() {
            RuntimeStudyRequest request = validRuntimeStudyRequest("study-session", "study-run");

            RuntimeStudyPointResponse point = mock(RuntimeStudyPointResponse.class);

            when(runExecutor.runBatch(any(RunRequest.class), eq(10), eq(0))).thenReturn(List.<RunGroupResponse>of());

            when(runStatisticsService.toRuntimeStudyPoint(eq(5), eq(List.of()))).thenReturn(point);

            runOrchestratorService.startRuntimeStudy(request);

            verify(runExecutor, timeout(2000)).runBatch(any(RunRequest.class), eq(10), eq(0));
            verify(runStatisticsService, timeout(2000)).toRuntimeStudyPoint(eq(5), eq(List.of()));
            verify(wsSender, timeout(2000).times(2)).sendToStudy(eq("study-run"), any(RuntimeStudyWsPayload.class));
        }

        @Test
        void startRuntimeStudy_sendsFailedPayloadWhenStudyFails() {
            RuntimeStudyRequest request = validRuntimeStudyRequest("study-failed-session", "study-failed-run");

            when(runExecutor.runBatch(any(RunRequest.class), eq(10), eq(0))).thenThrow(new RuntimeException("study boom"));

            runOrchestratorService.startRuntimeStudy(request);

            verify(runExecutor, timeout(2000)).runBatch(any(RunRequest.class), eq(10), eq(0));
            verify(wsSender, timeout(2000)).sendToStudy(eq("study-failed-run"), any(RuntimeStudyWsPayload.class));
        }
    }

    private static RunRequest validRunRequest(String sessionId, String runId) {
        return validRunRequest(sessionId, runId, 1, 1);
    }

    private static RunRequest validRunRequest(String sessionId, String runId, int logEveryIterations, int wsUpdateEveryIterations) {
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
            logEveryIterations,
            wsUpdateEveryIterations
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
            List.of(5),
            1
        );
    }
}