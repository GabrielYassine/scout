package dk.dtu.scout.backend.websocket;

import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.service.RunOrchestratorService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class WsReceiver {

    private final WsSender wsSender;
    private final RunOrchestratorService runOrchestratorService;

    public WsReceiver(WsSender wsSender, RunOrchestratorService runOrchestratorService) {
        this.wsSender = wsSender;
        this.runOrchestratorService = runOrchestratorService;
    }

    @MessageMapping("/run/{runId}/ready")
    public void ready(@DestinationVariable String runId) {
        wsSender.sendToRun(runId, RunWsPayload.connected(runId));
    }

    @MessageMapping("/run/{runId}/start")
    public void start(@DestinationVariable String runId, RunRequest request) {
        try {
            RunRequest normalized = normalizeRunId(runId, request);
            runOrchestratorService.startRun(normalized);
        } catch (Exception ex) {
            wsSender.sendToRun(runId, RunWsPayload.failed(runId, ex.getMessage()));
        }
    }

    private RunRequest normalizeRunId(String runId, RunRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Run request must be provided");
        }

        if (runId.equals(request.runId())) {
            return request;
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
                request.sessionId(),
                runId,
                request.logEveryIterations(),
                request.wsUpdateEveryIterations()
        );
    }
}