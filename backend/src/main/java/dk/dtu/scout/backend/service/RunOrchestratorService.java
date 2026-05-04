package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.request.PrepareRunRequest;
import dk.dtu.scout.backend.dto.request.PrepareRunResponse;
import dk.dtu.scout.backend.dto.request.RunRequest;
import dk.dtu.scout.backend.dto.request.RuntimeStudyRequest;
import dk.dtu.scout.backend.dto.run.BatchSummaryResponse;
import dk.dtu.scout.backend.dto.run.RunGroupResponse;
import dk.dtu.scout.backend.dto.run.RuntimeStudyPointResponse;
import dk.dtu.scout.backend.dto.ws.RunWsPayload;
import dk.dtu.scout.backend.dto.ws.RuntimeStudyWsPayload;
import dk.dtu.scout.backend.websocket.WsSender;
import dk.dtu.scout.backend.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * Facade for run orchestration.
 * Validates requests, prepares executions, starts asynchronous execution,
 * sends final websocket status messages, and exposes synchronous entry points for tests.
 * @author s235257 & Ahmed
 */
@Service
public class RunOrchestratorService {

    private final RunRequestValidator runRequestValidator;
    private final RunExecutor runExecutor;
    private final RunStatisticsService runStatisticsService;
    private final WsSender wsSender;
    private final ExecutionRegistry executionRegistry;
    private final ThreadPoolTaskExecutor requestExecutor;

    public RunOrchestratorService(
        RunRequestValidator runRequestValidator,
        RunExecutor runExecutor,
        RunStatisticsService runStatisticsService,
        WsSender wsSender,
        ExecutionRegistry executionRegistry,
        @Qualifier("requestExecutor") Executor requestExecutor
    ) {
        this.runRequestValidator = runRequestValidator;
        this.runExecutor = runExecutor;
        this.runStatisticsService = runStatisticsService;
        this.wsSender = wsSender;
        this.executionRegistry = executionRegistry;
        this.requestExecutor = (ThreadPoolTaskExecutor) requestExecutor;
    }

    /**
     * Prepares an execution by generating/reusing a sessionId, generating a new executionId,
     * applying those ids to the provided draft request, validating the final request, and
     * storing it until websocket start.
     * For a standard run, the executionId is used as the runId.
     * For a runtime study, the executionId is used as the studyId.
     * @param request draft prepare request containing either a run request or runtime study request
     * @return sessionId and executionId
     */
    public PrepareRunResponse prepareRun(PrepareRunRequest request) {
        ExecutionRegistry.PreparedExecutionIds ids = executionRegistry.prepareIds(request.sessionId());

        switch (normalizeExecutionType(request.executionType())) {
            case "run" -> {
                RunRequest finalRequest = withRunIds(request.runRequest(), ids.sessionId(), ids.executionId());
                runRequestValidator.validateRunRequest(finalRequest);
                executionRegistry.storePreparedRun(finalRequest);
            }

            case "runtimestudy" -> {
                RuntimeStudyRequest finalRequest = withStudyIds(request.runtimeStudyRequest(), ids.sessionId(), ids.executionId());
                runRequestValidator.validateRuntimeStudyRequest(finalRequest);
                executionRegistry.storePreparedStudy(finalRequest);
            }

            default -> throw new BadRequestException("executionType must be either 'run' or 'runtimeStudy'");
        }

        return new PrepareRunResponse(ids.sessionId(), ids.executionId());
    }

    private String normalizeExecutionType(String executionType) {
        return executionType == null ? "" : executionType.trim().replace("-", "").toLowerCase();
    }

    private RunRequest withRunIds(RunRequest request, String sessionId, String runId) {
        if (request == null) {
            throw new BadRequestException("Run request must be provided");
        }

        return new RunRequest(
            request.searchSpaceId(),
            request.searchSpaceParams(),
            request.problemIds(),
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
            request.observerIds(),
            request.observerParams(),
            request.stopConditionIds(),
            request.stopConditionParams(),
            request.seed(),
            request.runTimes(),
            sessionId,
            runId,
            request.logEveryIterations(),
            request.wsUpdateEveryIterations()
        );
    }

    private RuntimeStudyRequest withStudyIds(RuntimeStudyRequest request, String sessionId, String studyId) {
        if (request == null) {
            throw new BadRequestException("Runtime study request must be provided");
        }

        return new RuntimeStudyRequest(
            studyId,
            sessionId,
            request.searchSpaceId(),
            request.searchSpaceParams(),
            request.problemId(),
            request.problemParams(),
            request.generatorId(),
            request.generatorParams(),
            request.selectionRuleId(),
            request.selectionRuleParams(),
            request.populationModelId(),
            request.populationModelParams(),
            request.parentSelectionRuleId(),
            request.parentSelectionRuleParams(),
            request.crossoverId(),
            request.crossoverParams(),
            request.stopConditionIds(),
            request.stopConditionParams(),
            request.seed(),
            request.problemSizes(),
            request.repetitionsPerSize()
        );
    }

    /**
     * Starts a run asynchronously and registers it in the ExecutionRegistry.
     * @param request the run request containing all necessary information to execute the run
     */
    public void startRun(RunRequest request) {
        String sessionId = request.sessionId();
        String runId = request.runId();

        Future<?> future = requestExecutor.submit(() -> run(request));
        executionRegistry.registerActive(sessionId, runId, future);

        requestExecutor.execute(() -> {
            try {
                future.get();
            } catch (CancellationException ignored) {
                // The run was cancelled, likely because another run was started for the same session.
                // Cancellation is a normal lifecycle event, so no failed payload is sent here.
            } catch (InterruptedException e) {
                // Preserve the interrupt status so the executor can handle thread shutdown correctly.
                Thread.currentThread().interrupt();
            } catch (ExecutionException ignored) {
                // run() already emitted a failed payload if needed.
            } finally {
                executionRegistry.finishRun(sessionId, runId);
            }
        });
    }

    /**
     * Executes the run and sends a final websocket message with the summary or failure.
     * @param request the prepared run request containing all necessary information to execute the run
     */
    public void run(RunRequest request) {
        int logEvery = runRequestValidator.resolveLogEveryIterations(request);
        int wsUpdateEvery = request.wsUpdateEveryIterations() > 0 ? request.wsUpdateEveryIterations() : logEvery;

        try {
            List<RunGroupResponse> batches = runExecutor.runBatch(request, logEvery, wsUpdateEvery);
            BatchSummaryResponse summary = runStatisticsService.calculateSummary(batches);
            wsSender.sendToRun(request.runId(), RunWsPayload.finished(request.runId(), request.searchSpaceId(), summary));
        } catch (CancellationException ex) {
            throw ex;
        } catch (Exception ex) {
            wsSender.sendToRun(request.runId(), RunWsPayload.failed(request.runId(), ex.getMessage()));

            throw ex;
        }
    }

    /**
     * Starts a runtime study asynchronously and registers it in the ExecutionRegistry.
     * @param request the runtime study request containing all necessary information to execute the study
     */
    public void startRuntimeStudy(RuntimeStudyRequest request) {
        String sessionId = request.sessionId();
        String studyId = request.studyId();

        Future<?> future = requestExecutor.submit(() -> runRuntimeStudy(request));
        executionRegistry.registerActive(sessionId, studyId, future);

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
                executionRegistry.finishStudy(sessionId, studyId);
            }
        });
    }

    /**
     * Synchronous runtime study execution for testing purposes.
     * Updates frontend with progress after each problem size is completed,
     * not after each log update as in normal runs.
     * @param request the runtime study request containing all necessary information to execute the study
     */
    public void runRuntimeStudy(RuntimeStudyRequest request) {
        runRequestValidator.validateRuntimeStudyRequest(request);

        try {
            List<Integer> sizes = request.problemSizes();

            for (int i = 0; i < sizes.size(); i++) {
                int n = sizes.get(i);
                RunRequest runRequest = buildRunRequestForSize(request, n, i);
                int logEvery = runRequestValidator.resolveLogEveryIterations(runRequest);

                List<RunGroupResponse> batches = runExecutor.runBatch(runRequest, logEvery, 0);
                RuntimeStudyPointResponse point = runStatisticsService.toRuntimeStudyPoint(n, batches);

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

    /**
     * Builds a RunRequest for a specific problem size as part of a runtime study.
     * @param request original runtime study request
     * @param problemSize current problem size
     * @param sizeIndex index of the current problem size
     * @return run request for this size
     */
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
}