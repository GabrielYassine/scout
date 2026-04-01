package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.run.BatchRunResponse;
import dk.dtu.scout.backend.websocket.RunStatusService;
import dk.dtu.scout.backend.websocket.RunWsPayload;
import dk.dtu.scout.backend.websocket.WsSender;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Facade for run orchestration. Validates requests, triggers async execution,
 * and exposes a synchronous entry point for tests.
 */
@Service
public class RunOrchestratorService {

    private final RunRequestValidator validator;
    private final RunExecutor runExecutor;
    private final WsSender wsSender;
    private final RunStatusService runStatusService;
    private final Executor requestExecutor;

    public RunOrchestratorService(
            RunRequestValidator validator,
            RunExecutor runExecutor,
            WsSender wsSender,
            RunStatusService runStatusService,
            @Qualifier("requestExecutor") Executor requestExecutor
    ) {
        this.validator = validator;
        this.runExecutor = runExecutor;
        this.wsSender = wsSender;
        this.runStatusService = runStatusService;
        this.requestExecutor = requestExecutor;
    }

    public void startRun(RunRequest request) {
        CompletableFuture.runAsync(() -> run(request), requestExecutor)
                .exceptionally(ex -> null);
    }

    public BatchRunResponse run(RunRequest request) {
        validator.validate(request);
        int logEvery = validator.resolveLogEveryIterations(request);
        int wsUpdateEvery = request.wsUpdateEveryIterations() > 0 ? request.wsUpdateEveryIterations() : logEvery;
        try {
            return runExecutor.executeBatch(request, logEvery, wsUpdateEvery);
        } catch (Exception ex) {
            if (request != null && request.runId() != null) {
                runStatusService.markFailed(request.runId(), ex.getMessage());
                wsSender.sendToRun(request.runId(), RunWsPayload.failed(request.runId(), ex.getMessage()));
            }
            throw ex;
        }
    }
}
