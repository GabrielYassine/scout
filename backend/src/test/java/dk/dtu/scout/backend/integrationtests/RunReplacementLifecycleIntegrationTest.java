package dk.dtu.scout.backend.integrationtests;

import dk.dtu.scout.backend.dto.request.RunRequest;
import dk.dtu.scout.backend.dto.run.BatchSummaryResponse;
import dk.dtu.scout.backend.dto.run.RunGroupResponse;
import dk.dtu.scout.backend.dto.ws.RunWsPayload;
import dk.dtu.scout.backend.service.RunExecutor;
import dk.dtu.scout.backend.service.RunOrchestratorService;
import dk.dtu.scout.backend.service.RunStatisticsService;
import dk.dtu.scout.backend.websocket.WsSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
class RunReplacementLifecycleIntegrationTest {

    @Autowired
    private RunOrchestratorService runOrchestratorService;

    @MockBean
    private RunExecutor runExecutor;

    @MockBean
    private RunStatisticsService runStatisticsService;

    @MockBean
    private WsSender wsSender;

    @Test
    void startingSecondRunForSameSessionCancelsPreviousActiveRun() throws Exception {
        String sessionId = "replacement-session";

        RunRequest firstRun = validRunRequest(sessionId, "replacement-run-1");
        RunRequest secondRun = validRunRequest(sessionId, "replacement-run-2");

        CountDownLatch firstRunStarted = new CountDownLatch(1);
        CountDownLatch firstRunWasInterrupted = new CountDownLatch(1);

        when(runExecutor.runBatch(eq(firstRun), eq(1), eq(1))).thenAnswer(invocation -> {
            firstRunStarted.countDown();

            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                firstRunWasInterrupted.countDown();
                Thread.currentThread().interrupt();
                throw new java.util.concurrent.CancellationException("first run cancelled");
            }

            return List.<RunGroupResponse>of();
        });

        when(runExecutor.runBatch(eq(secondRun), eq(1), eq(1)))
            .thenReturn(List.<RunGroupResponse>of());

        BatchSummaryResponse summary = mock(BatchSummaryResponse.class);
        when(runStatisticsService.calculateSummary(List.of())).thenReturn(summary);

        runOrchestratorService.startRun(firstRun);

        assert firstRunStarted.await(2, TimeUnit.SECONDS);

        runOrchestratorService.startRun(secondRun);

        assert firstRunWasInterrupted.await(2, TimeUnit.SECONDS);

        verify(runExecutor, timeout(2000)).runBatch(eq(firstRun), eq(1), eq(1));
        verify(runExecutor, timeout(2000)).runBatch(eq(secondRun), eq(1), eq(1));

        verify(wsSender, timeout(2000))
            .sendToRun(eq("replacement-run-2"), any(RunWsPayload.class));

        verify(wsSender, after(500).never())
            .sendToRun(eq("replacement-run-1"), any(RunWsPayload.class));
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
            List.of("max-evaluations"),
            Map.of("maxEvaluations", 10),
            1234L,
            1,
            sessionId,
            runId,
            1,
            1
        );
    }
}