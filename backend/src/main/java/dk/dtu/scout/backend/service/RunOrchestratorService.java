package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.PrepareRunResponse;
import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.RuntimeStudyRequest;
import dk.dtu.scout.backend.dto.run.BatchRunResponse;
import dk.dtu.scout.backend.dto.study.RuntimeStudyPointResponse;
import dk.dtu.scout.backend.websocket.RunWsPayload;
import dk.dtu.scout.backend.websocket.RuntimeStudyWsPayload;
import dk.dtu.scout.backend.websocket.WsSender;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * Facade for run orchestration.
 * Validates requests, starts asynchronous execution, sends final websocket status messages,
 * and exposes synchronous entry points for tests.
 * @author s235257 & Ahmed
 */
@Service
public class RunOrchestratorService {

    private final RunRequestValidator runRequestValidator;
    private final RunExecutor runExecutor;
    private final RunStatisticsService runStatisticsService;
    private final WsSender wsSender;
    private final ActiveRunRegistry activeRunRegistry;
    private final ThreadPoolTaskExecutor requestExecutor;

    public RunOrchestratorService(
        RunRequestValidator runRequestValidator,
        RunExecutor runExecutor,
        RunStatisticsService runStatisticsService,
        WsSender wsSender,
        ActiveRunRegistry activeRunRegistry,
        @Qualifier("requestExecutor") Executor requestExecutor
    ) {
        this.runRequestValidator = runRequestValidator;
        this.runExecutor = runExecutor;
        this.runStatisticsService = runStatisticsService;
        this.wsSender = wsSender;
        this.activeRunRegistry = activeRunRegistry;
        this.requestExecutor = (ThreadPoolTaskExecutor) requestExecutor;
    }

    public PrepareRunResponse prepareRun(String requestedSessionId) {
        String sessionId = requestedSessionId != null && !requestedSessionId.isBlank() ? requestedSessionId : UUID.randomUUID().toString();
        String runId = UUID.randomUUID().toString();
        return new PrepareRunResponse(sessionId, runId);
    }

    public void startRun(RunRequest request) {
        runRequestValidator.validateRunRequest(request);

        String sessionId = requireText(request.sessionId(), "sessionId is required");
        String runId = requireText(request.runId(), "runId is required");

        if (!activeRunRegistry.markRunStarted(runId)) {
            return;
        }

        Future<?> future = requestExecutor.submit(() -> run(request));
        activeRunRegistry.register(sessionId, runId, future);

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
                activeRunRegistry.finishRun(sessionId, runId);
            }
        });
    }

    public BatchRunResponse run(RunRequest request) {
        runRequestValidator.validateRunRequest(request);

        int logEvery = runRequestValidator.resolveLogEveryIterations(request);
        int wsUpdateEvery = request.wsUpdateEveryIterations() > 0 ? request.wsUpdateEveryIterations() : logEvery;
        try {
            BatchRunResponse response = runExecutor.runBatch(request, logEvery, wsUpdateEvery);
            if (request.runId() != null) {
                wsSender.sendToRun(request.runId(), RunWsPayload.finished(request.runId(), request.searchSpaceId(), response.summary()));
            }
            return response;
        } catch (CancellationException ex) {
            throw ex;
        } catch (Exception ex) {
            if (request.runId() != null) {
                wsSender.sendToRun(request.runId(), RunWsPayload.failed(request.runId(), ex.getMessage()));
            }
            throw ex;
        }
    }

    public void startRuntimeStudy(RuntimeStudyRequest request) {
        runRequestValidator.validateRuntimeStudyRequest(request);

        String sessionId = requireText(request.sessionId(), "sessionId is required");
        String studyId = requireText(request.studyId(), "studyId is required");

        if (!activeRunRegistry.markStudyStarted(studyId)) {
            return;
        }

        Future<?> future = requestExecutor.submit(() -> runRuntimeStudy(request));
        activeRunRegistry.register(sessionId, studyId, future);

        requestExecutor.execute(() -> {
            try {
                future.get();
            } catch (CancellationException ignored) {
                // cancelled
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException ignored) {
                // runRuntimeStudy already emits failed payload if needed
            } finally {
                activeRunRegistry.finishStudy(sessionId, studyId);
            }
        });
    }

    public void runRuntimeStudy(RuntimeStudyRequest request) {
        runRequestValidator.validateRuntimeStudyRequest(request);
        try {
            List<Integer> sizes = request.problemSizes();
            for (int i = 0; i < sizes.size(); i++) {
                int n = sizes.get(i);
                RunRequest runRequest = buildRunRequestForSize(request, n, i);
                int logEvery = runRequestValidator.resolveLogEveryIterations(runRequest);
                BatchRunResponse batch = runExecutor.runBatch(runRequest, logEvery, 0);

                RuntimeStudyPointResponse point = runStatisticsService.toRuntimeStudyPoint(n, batch);
                wsSender.sendToStudy(request.studyId(), RuntimeStudyWsPayload.progress(request.studyId(), point));
            }
            wsSender.sendToStudy(request.studyId(), RuntimeStudyWsPayload.finished(request.studyId()));
        } catch (CancellationException ex) {
            throw ex;
        } catch (Exception ex) {
            wsSender.sendToStudy(request.studyId(), RuntimeStudyWsPayload.failed(request.studyId(), ex.getMessage()));
            throw ex;
        }
    }

    private RunRequest buildRunRequestForSize(RuntimeStudyRequest request, int problemSize, int sizeIndex) {
        Map<String, Object> searchSpaceParams = new LinkedHashMap<>(request.searchSpaceParams() != null ? request.searchSpaceParams() : Map.of());

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

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }
}