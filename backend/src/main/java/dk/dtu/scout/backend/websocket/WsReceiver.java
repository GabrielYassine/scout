package dk.dtu.scout.backend.websocket;

import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.RuntimeStudyRequest;
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
    public void runReady(@DestinationVariable String runId) {
        wsSender.sendToRun(runId, RunWsPayload.connected(runId));
    }

    @MessageMapping("/run/{runId}/start")
    public void runStart(@DestinationVariable String runId, RunRequest request) {
        try {
            RunRequest normalized = normalizeRunId(runId, request);
            runOrchestratorService.startRun(normalized);
        } catch (Exception ex) {
            wsSender.sendToRun(runId, RunWsPayload.failed(runId, ex.getMessage()));
        }
    }

    @MessageMapping("/study/{studyId}/ready")
    public void studyReady(@DestinationVariable String studyId) {
        wsSender.sendToStudy(studyId, RuntimeStudyWsPayload.connected(studyId));
    }

    @MessageMapping("/study/{studyId}/start")
    public void studyStart(@DestinationVariable String studyId, RuntimeStudyRequest request) {
        try {
            RuntimeStudyRequest normalized = normalizeStudyId(studyId, request);
            runOrchestratorService.startRuntimeStudy(normalized);
        } catch (Exception ex) {
            wsSender.sendToStudy(studyId, RuntimeStudyWsPayload.failed(studyId, ex.getMessage()));
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

    private RuntimeStudyRequest normalizeStudyId(String studyId, RuntimeStudyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Runtime study request must be provided");
        }

        if (studyId.equals(request.studyId())) {
            return request;
        }

        return new RuntimeStudyRequest(
                studyId,
                request.sessionId(),
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
}