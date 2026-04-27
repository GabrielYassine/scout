package dk.dtu.scout.backend.websocket;

import dk.dtu.scout.backend.dto.request.RunRequest;
import dk.dtu.scout.backend.dto.request.RuntimeStudyRequest;
import dk.dtu.scout.backend.dto.ws.RunWsPayload;
import dk.dtu.scout.backend.dto.ws.RuntimeStudyWsPayload;
import dk.dtu.scout.backend.service.RunOrchestratorService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * Receives WebSocket messages from the frontend.
 * The frontend first sends a ready message after subscribing, and then sends a start message
 * to begin either a normal run or a runtime study.
 *
 * @author s235257 & Ahmed
 */
@Controller
public class WsReceiver {

    private final WsSender wsSender;
    private final RunOrchestratorService runOrchestratorService;

    public WsReceiver(WsSender wsSender, RunOrchestratorService runOrchestratorService) {
        this.wsSender = wsSender;
        this.runOrchestratorService = runOrchestratorService;
    }

    /**
     * Confirms that the frontend is connected to the run topic.
     * @param runId the run ID from the WebSocket destination
     */
    @MessageMapping("/run/{runId}/ready")
    public void runReady(@DestinationVariable String runId) {
        wsSender.sendToRun(runId, RunWsPayload.connected(runId));
    }

    /**
     * Starts a normal run after the frontend has subscribed to its run topic.
     * @param runId the run ID from the WebSocket destination
     * @param request the run request sent by the frontend
     */
    @MessageMapping("/run/{runId}/start")
    public void runStart(@DestinationVariable String runId, RunRequest request) {
        try {
            RunRequest normalized = normalizeRunId(runId, request);
            runOrchestratorService.startRun(normalized);
        } catch (Exception ex) {
            wsSender.sendToRun(runId, RunWsPayload.failed(runId, ex.getMessage()));
        }
    }

    /**
     * Confirms that the frontend is connected to the runtime study topic.
     * @param studyId the study ID from the WebSocket destination
     */
    @MessageMapping("/study/{studyId}/ready")
    public void studyReady(@DestinationVariable String studyId) {
        wsSender.sendToStudy(studyId, RuntimeStudyWsPayload.connected(studyId));
    }

    /**
     * Starts a runtime study after the frontend has subscribed to its study topic.
     * @param studyId the study ID from the WebSocket destination
     * @param request the runtime study request sent by the frontend
     */
    @MessageMapping("/study/{studyId}/start")
    public void studyStart(@DestinationVariable String studyId, RuntimeStudyRequest request) {
        try {
            RuntimeStudyRequest normalized = normalizeStudyId(studyId, request);
            runOrchestratorService.startRuntimeStudy(normalized);
        } catch (Exception ex) {
            wsSender.sendToStudy(studyId, RuntimeStudyWsPayload.failed(studyId, ex.getMessage()));
        }
    }

    /**
     * Ensures that the run ID in the destination is used as the source of truth.
     * This avoids mismatches if the payload contains a different run ID.
     * @param runId the run ID from the WebSocket destination
     * @param request the run request sent by the frontend
     * @return request with the destination run ID applied
     */
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

    /**
     * Ensures that the study ID in the destination is used as the source of truth.
     * This avoids mismatches if the payload contains a different study ID.
     * @param studyId the study ID from the WebSocket destination
     * @param request the runtime study request sent by the frontend
     * @return request with the destination study ID applied
     */
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