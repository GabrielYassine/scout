package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.request.PrepareRunResponse;
import dk.dtu.scout.backend.dto.request.RunRequest;
import dk.dtu.scout.backend.dto.request.RuntimeStudyRequest;
import dk.dtu.scout.backend.dto.run.BatchSummaryResponse;
import dk.dtu.scout.backend.dto.run.RunGroupResponse;
import dk.dtu.scout.backend.dto.run.RuntimeStudyPointResponse;
import dk.dtu.scout.backend.dto.ws.RunWsPayload;
import dk.dtu.scout.backend.dto.ws.RuntimeStudyWsPayload;
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

    /**
     * Prepares for a run by generating a runId and returning it along with the provided or generated sessionId.
     * @param requestedSessionId an optional sessionId, if null or blank, a new random sessionId will be generated.
     * @return a PrepareRunResponse containing the sessionId and a new runId to be used for the run request.
     */
    public PrepareRunResponse prepareRun(String requestedSessionId) {
        String sessionId = requestedSessionId != null && !requestedSessionId.isBlank() ? requestedSessionId : UUID.randomUUID().toString();

        String runId = UUID.randomUUID().toString();
        return new PrepareRunResponse(sessionId, runId);
    }

    /**
     * Starts a run asynchronously. Validates the request, registers the run in the ActiveRunRegistry,
     * @param request the run request containing all necessary information to execute the run.
     */
    public void startRun(RunRequest request) {
        runRequestValidator.validateRunRequest(request);

        String sessionId = request.sessionId();
        String runId = request.runId();

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

    /**
     * Synchronous run execution for testing purposes.
     * Validates the request
     * executes the run
     * and sends a final websocket message with the summary or failure.
     * @param request the run request containing all necessary information to execute the run.
     */
    public void run(RunRequest request) {
        runRequestValidator.validateRunRequest(request);

        int logEvery = runRequestValidator.resolveLogEveryIterations(request);
        int wsUpdateEvery = request.wsUpdateEveryIterations() > 0 ? request.wsUpdateEveryIterations() : logEvery;

        try {
            // Main execution
            List<RunGroupResponse> batches = runExecutor.runBatch(request, logEvery, wsUpdateEvery);

            // Calculate summary statistics
            BatchSummaryResponse summary = runStatisticsService.calculateSummary(batches);

            if (request.runId() != null) {
                wsSender.sendToRun(request.runId(), RunWsPayload.finished(request.runId(), request.searchSpaceId(), summary));
            }
        } catch (CancellationException ex) {
            throw ex;
        } catch (Exception ex) {
            if (request.runId() != null) {
                wsSender.sendToRun(request.runId(), RunWsPayload.failed(request.runId(), ex.getMessage()));
            }

            throw ex;
        }
    }

    /**
     * Starts a runtime study asynchronously. Validates the request, registers the study in the ActiveRunRegistry,
     * and executes the study by iterating over the specified problem sizes and sending progress updates through
     * @param request the runtime study request containing all necessary information to execute the study.
     */
    public void startRuntimeStudy(RuntimeStudyRequest request) {
        runRequestValidator.validateRuntimeStudyRequest(request);

        String sessionId = request.sessionId();
        String studyId = request.studyId();

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

    /**
     * Synchronous runtime study execution for testing purposes.
     * Updates frontend with progress after each problem size is completed, not after each log update as in normal runs.
     * @param request the runtime study request containing all necessary information to execute the study.
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
     * Helper method to build a RunRequest for a specific problem size based on the RuntimeStudyRequest.
     * @param request the original runtime study request containing the common parameters for all runs in the study.
     * @param problemSize the specific problem size for this run, which will be added to the search space parameters as "n".
     * @param sizeIndex the index of the problem size in the list, used to derive a unique seed for each run to ensure different randomization across runs.
     * @return a RunRequest configured for the specific problem size, ready to be executed as part of the runtime study.
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