package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.RuntimeStudyRequest;
import dk.dtu.scout.backend.dto.run.BatchRunResponse;
import dk.dtu.scout.backend.dto.study.RuntimeStudyPointResponse;
import dk.dtu.scout.backend.dto.study.RuntimeStudyResponse;
import dk.dtu.scout.backend.websocket.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Facade for run orchestration. Validates requests, triggers async execution,
 * and exposes a synchronous entry point for tests.
 */
@Service
public class RunOrchestratorService {

    private final RunRequestValidator runRequestValidator;
    private final RuntimeStudyRequestValidator runtimeStudyRequestValidator;
    private final RunExecutor runExecutor;
    private final StatisticsService statisticsService;
    private final WsSender wsSender;
    private final RunStatusService runStatusService;
    private final Executor requestExecutor;

    public RunOrchestratorService(
            RunRequestValidator runRequestValidator,
            RuntimeStudyRequestValidator runtimeStudyRequestValidator,
            RunExecutor runExecutor,
            StatisticsService statisticsService,
            WsSender wsSender,
            RunStatusService runStatusService,
            @Qualifier("requestExecutor") Executor requestExecutor
    ) {
        this.runRequestValidator = runRequestValidator;
        this.runtimeStudyRequestValidator = runtimeStudyRequestValidator;
        this.runExecutor = runExecutor;
        this.statisticsService = statisticsService;
        this.wsSender = wsSender;
        this.runStatusService = runStatusService;
        this.requestExecutor = requestExecutor;
    }

    public void startRun(RunRequest request) {
        runRequestValidator.validate(request);
        CompletableFuture.runAsync(() -> run(request), requestExecutor).exceptionally(ex -> null);
    }

    public BatchRunResponse run(RunRequest request) {
        runRequestValidator.validate(request);
        int logEvery = runRequestValidator.resolveLogEveryIterations(request); // When we log progress in the backend
        int wsUpdateEvery = request.wsUpdateEveryIterations() > 0 ? request.wsUpdateEveryIterations() : logEvery; // When we update frontend
        try {
            BatchRunResponse response = runExecutor.executeBatch(request, logEvery, wsUpdateEvery);
            if (request.runId() != null) {
                runStatusService.markFinished(request.runId(), response);
                wsSender.sendToRun(request.runId(), RunWsPayload.finished(request.runId(), response));
            }
            return response;
        } catch (Exception ex) {
            if (request.runId() != null) {
                runStatusService.markFailed(request.runId(), ex.getMessage());
                wsSender.sendToRun(request.runId(), RunWsPayload.failed(request.runId(), ex.getMessage()));
            }
            throw ex;
        }
    }

    public void startRuntimeStudy(RuntimeStudyRequest request) {
        runtimeStudyRequestValidator.validate(request);
        System.out.println("Starting runtime study with ID: " + request.studyId());
        CompletableFuture.runAsync(() -> runRuntimeStudy(request), requestExecutor).exceptionally(ex -> {
            wsSender.sendToStudy(
                    request.studyId(),
                    RuntimeStudyWsPayload.failed(request.studyId(), ex.getMessage())
            );
            ex.printStackTrace();
            return null;
        });
    }

    public RuntimeStudyResponse runRuntimeStudy(RuntimeStudyRequest request) {
        runtimeStudyRequestValidator.validate(request);

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
                0
        );
    }

}
