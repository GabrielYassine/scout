package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.PrepareRunResponse;
import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.RuntimeStudyRequest;
import dk.dtu.scout.backend.dto.run.BatchRunResponse;
import dk.dtu.scout.backend.dto.run.RunFinalResponse;
import dk.dtu.scout.backend.dto.study.RuntimeStudyPointResponse;
import dk.dtu.scout.backend.dto.study.RuntimeStudyResponse;
import dk.dtu.scout.backend.websocket.RunWsPayload;
import dk.dtu.scout.backend.websocket.RuntimeStudyWsPayload;
import dk.dtu.scout.backend.websocket.WsSender;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    /**
     * Guards against duplicate websocket start messages for the same run or study, which can happen when a client reconnects.
     */
    private final ConcurrentHashMap<String, Boolean> startedRunIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> startedStudyIds = new ConcurrentHashMap<>();

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

    public PrepareRunResponse prepareRun(String requestedSessionId) {
        String sessionId =
                requestedSessionId != null && !requestedSessionId.isBlank()
                        ? requestedSessionId
                        : UUID.randomUUID().toString();

        String runId = UUID.randomUUID().toString();
        return new PrepareRunResponse(sessionId, runId);
    }

    public boolean startRun(RunRequest request) {
        runRequestValidator.runRequestValidator(request);

        String sessionId = request.sessionId();
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }

        String runId = request.runId();
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("runId is required");
        }

        if (startedRunIds.putIfAbsent(runId, Boolean.TRUE) != null) {
            return false;
        }

        ActiveTask previous = activeBySession.get(sessionId);
        if (previous != null && previous.future != null) {
            previous.future.cancel(true);
        }

        Future<?> future = requestExecutor.submit(() -> run(request));

        ActiveTask current = new ActiveTask(runId, future);
        activeBySession.put(sessionId, current);

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
                startedRunIds.remove(runId);
                activeBySession.computeIfPresent(
                        sessionId,
                        (sid, active) -> runId.equals(active.id()) ? null : active
                );
            }
        });

        return true;
    }

    public BatchRunResponse run(RunRequest request) {
        runRequestValidator.runRequestValidator(request);
        int logEvery = runRequestValidator.resolveLogEveryIterations(request);
        int wsUpdateEvery =
                request.wsUpdateEveryIterations() > 0
                        ? request.wsUpdateEveryIterations()
                        : logEvery;

        try {
            BatchRunResponse response = runExecutor.executeBatch(request, logEvery, wsUpdateEvery);
            if (request.runId() != null) {
                List<RunFinalResponse> completedRuns = response.batches().stream()
                        .flatMap(batch -> batch.runs().stream().map(run ->
                                new RunFinalResponse(batch.runIndex(), run.problemId(), run.runtimeMs())))
                        .toList();

                wsSender.sendToRun(
                        request.runId(),
                        RunWsPayload.finished(request.runId(), response.summary(), completedRuns)
                );
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

    public boolean startRuntimeStudy(RuntimeStudyRequest request) {
        runRequestValidator.runtimeStudyRequestValidator(request);

        String sessionId = request.sessionId();
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }

        String studyId = request.studyId();
        if (studyId == null || studyId.isBlank()) {
            throw new IllegalArgumentException("studyId is required");
        }

        if (startedStudyIds.putIfAbsent(studyId, Boolean.TRUE) != null) {
            return false;
        }

        ActiveTask previous = activeBySession.get(sessionId);
        if (previous != null && previous.future != null) {
            previous.future.cancel(true);
        }

        Future<?> future = requestExecutor.submit(() -> runRuntimeStudy(request));

        ActiveTask current = new ActiveTask(studyId, future);
        activeBySession.put(sessionId, current);

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
                startedStudyIds.remove(studyId);
                activeBySession.computeIfPresent(
                        sessionId,
                        (sid, active) -> studyId.equals(active.id()) ? null : active
                );
            }
        });

        return true;
    }

    public RuntimeStudyResponse runRuntimeStudy(RuntimeStudyRequest request) {
        runRequestValidator.runtimeStudyRequestValidator(request);
        try {
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
        } catch (CancellationException ex) {
            throw ex;
        } catch (Exception ex) {
            wsSender.sendToStudy(request.studyId(), RuntimeStudyWsPayload.failed(request.studyId(), ex.getMessage()));
            throw ex;
        }
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