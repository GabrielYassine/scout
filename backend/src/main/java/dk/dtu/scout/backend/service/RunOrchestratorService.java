package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.RuntimeStudyRequest;
import dk.dtu.scout.backend.dto.run.BatchRunResponse;
import dk.dtu.scout.backend.dto.study.RuntimeStudyPointResponse;
import dk.dtu.scout.backend.dto.study.RuntimeStudyResponse;
import dk.dtu.scout.backend.websocket.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Facade for run orchestration. Validates requests, triggers async execution,
 * and exposes a synchronous entry point for tests.
 */
@Service
public class RunOrchestratorService {

    private record ActiveTask(String id, Future<?> future) {}

    private final RunRequestValidator runRequestValidator;
    private final RunExecutor runExecutor;
    private final StatisticsService statisticsService;
    private final WsSender wsSender;
    private final ThreadPoolTaskExecutor requestExecutor;

    /**
     * Active run per client session (tab-scoped).
     */
    private final ConcurrentHashMap<String, ActiveTask> activeBySession = new ConcurrentHashMap<>();

    public RunOrchestratorService(
            RunRequestValidator runRequestValidator,
            RunExecutor runExecutor,
            StatisticsService statisticsService,
            WsSender wsSender,
            @Qualifier("requestExecutor") Executor requestExecutor
    ) {
        this.runRequestValidator = runRequestValidator;
        this.runExecutor = runExecutor;
        this.statisticsService = statisticsService;
        this.wsSender = wsSender;
        this.requestExecutor = (ThreadPoolTaskExecutor) requestExecutor;
    }

    public void startRun(RunRequest request) {
        runRequestValidator.runRequestValidator(request);

        String sessionId = request.sessionId();
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }
        String runId = request.runId();

        ActiveTask previous = activeBySession.get(sessionId);
        if (previous != null) {
            previous.future.cancel(true);
        }

        Future<?> future = requestExecutor.submit(() -> run(request));

        ActiveTask current = new ActiveTask(runId, future);
        activeBySession.put(sessionId, current);

        // Cleanup: remove only if this run is still the active one for the session.
        requestExecutor.execute(() -> {
            try {
                future.get();
            } catch (CancellationException ignored) {
                // cancelled
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException ignored) {
                // run() already emitted failed payload if needed
            } finally {
                activeBySession.computeIfPresent(sessionId, (sid, active) -> runId.equals(active.id()) ? null : active);
            }
        });
    }

    public BatchRunResponse run(RunRequest request) {
        runRequestValidator.runRequestValidator(request);
        int logEvery = runRequestValidator.resolveLogEveryIterations(request); // When we log progress in the backend
        int wsUpdateEvery = request.wsUpdateEveryIterations() > 0 ? request.wsUpdateEveryIterations() : logEvery; // When we update frontend
        try {
            BatchRunResponse response = runExecutor.executeBatch(request, logEvery, wsUpdateEvery);
            if (request.runId() != null) {
                wsSender.sendToRun(request.runId(), RunWsPayload.finished(request.runId(), response.summary()));
            }
            return response;
        } catch (CancellationException ex) {
            // Task was superseded/cancelled. Don't emit failed; client should ignore stale runIds.
            throw ex;
        } catch (Exception ex) {
            if (request.runId() != null) {
                wsSender.sendToRun(request.runId(), RunWsPayload.failed(request.runId(), ex.getMessage()));
            }
            throw ex;
        }
    }

    public void startRuntimeStudy(RuntimeStudyRequest request) {
        runRequestValidator.runtimeStudyRequestValidator(request);

        String sessionId = request.sessionId();
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }
        String studyId = request.studyId();

        ActiveTask previous = activeBySession.get(sessionId);
        if (previous != null) {
            previous.future.cancel(true);
        }

        Future<?> future = requestExecutor.submit(() -> runRuntimeStudy(request));

        ActiveTask current = new ActiveTask(studyId, future);
        activeBySession.put(sessionId, current);

        // Cleanup: remove only if this run is still the active one for the session.
        requestExecutor.execute(() -> {
            try {
                future.get();
            } catch (CancellationException ignored) {
                // cancelled
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException ignored) {
                // run() already emitted failed payload if needed
            } finally {
                activeBySession.computeIfPresent(sessionId, (sid, active) -> studyId.equals(active.id()) ? null : active);
            }
        });
    }

    public RuntimeStudyResponse runRuntimeStudy(RuntimeStudyRequest request) {
        runRequestValidator.runtimeStudyRequestValidator(request);

        List<RuntimeStudyPointResponse> points = new ArrayList<>();
        List<Integer> sizes = request.problemSizes();

        for (int i = 0; i < sizes.size(); i++) {
            int n = sizes.get(i);
            RunRequest runRequest = buildRunRequestForSize(request, n, i);
            int logEvery = runRequestValidator.resolveLogEveryIterations(runRequest);
            BatchRunResponse batch = runExecutor.executeBatch(runRequest, logEvery, 0);
            RuntimeStudyPointResponse point = statisticsService.toRuntimeStudyPoint(n, batch);
            points.add(point);
        }
        RuntimeStudyResponse response = new RuntimeStudyResponse(request.studyId(), points);
        wsSender.sendToStudy(request.studyId(), RuntimeStudyWsPayload.finished(request.studyId(), response));
        return response;
    }

    private RunRequest buildRunRequestForSize(RuntimeStudyRequest request, int problemSize, int sizeIndex) {
        Map<String, Object> searchSpaceParams = new LinkedHashMap<>(
                request.searchSpaceParams() != null ? request.searchSpaceParams() : Map.of()
        );
        searchSpaceParams.put("n", problemSize);

        long seed = request.seed() + (long) sizeIndex * 1_000_000L;

        return new RunRequest(
                request.searchSpaceId(),
                searchSpaceParams,
                List.of(request.problemId()),
                request.problemParams(),
                request.generatorId(),
                request.generatorParams(),
                request.populationModelId(),
                request.populationModelParams(),
                request.selectionRuleId(),
                request.selectionRuleParams(),
                request.parentSelectionRuleId(),
                request.parentSelectionRuleParams(),
                request.crossoverId(),
                request.crossoverParams(),
                List.of(),
                Map.of(),
                request.stopConditionIds(),
                request.stopConditionParams(),
                seed,
                request.repetitionsPerSize(),
                null,
                null,
                0,
                0
        );
    }

}
