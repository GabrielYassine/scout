package dk.dtu.scout.backend.websocket;

import dk.dtu.scout.backend.dto.request.RunRequest;
import dk.dtu.scout.backend.dto.request.RuntimeStudyRequest;
import dk.dtu.scout.backend.dto.request.StartPreparedExecutionRequest;
import dk.dtu.scout.backend.dto.ws.RunWsPayload;
import dk.dtu.scout.backend.dto.ws.RuntimeStudyWsPayload;
import dk.dtu.scout.backend.service.ExecutionRegistry;
import dk.dtu.scout.backend.service.RunOrchestratorService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * Receives WebSocket messages from the frontend.
 * The frontend subscribes to run/study topics, then sends a start message
 * containing only the session id of a prepared backend execution.
 * The execution id itself comes from the websocket destination path, so the backend
 * does not trust a runId or studyId from the message body.
 * @author s235257 & Ahmed
 */
@Controller
public class WsReceiver {

    private final WsSender wsSender;
    private final RunOrchestratorService runOrchestratorService;
    private final ExecutionRegistry executionRegistry;

    public WsReceiver(WsSender wsSender, RunOrchestratorService runOrchestratorService, ExecutionRegistry executionRegistry) {
        this.wsSender = wsSender;
        this.runOrchestratorService = runOrchestratorService;
        this.executionRegistry = executionRegistry;
    }

    /**
     * Starts a prepared normal run after the frontend has subscribed to its run topic.
     * The full RunRequest was already validated and stored during REST prepare.
     * @param runId the run id from the websocket destination
     * @param request small request containing the browser session id
     * @param headers websocket message headers
     */
    @MessageMapping("/run/{runId}/start")
    public void runStart(@DestinationVariable String runId, StartPreparedExecutionRequest request, SimpMessageHeaderAccessor headers) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("Start request must be provided");
            }

            executionRegistry.attachRunWebSocket(headers.getSessionId(), request.sessionId(), runId);
            RunRequest preparedRequest = executionRegistry.consumePreparedRun(runId, request.sessionId());
            runOrchestratorService.startRun(preparedRequest);
        } catch (Exception ex) {
            wsSender.sendToRun(runId, RunWsPayload.failed(runId, ex.getMessage()));
        }
    }


    /**
     * Starts a prepared runtime study after the frontend has subscribed to its study topic.
     * The full RuntimeStudyRequest was already validated and stored during REST prepare.
     * @param studyId the study id from the websocket destination
     * @param request small request containing the browser session id
     * @param headers websocket message headers
     */
    @MessageMapping("/study/{studyId}/start")
    public void studyStart(@DestinationVariable String studyId, StartPreparedExecutionRequest request, SimpMessageHeaderAccessor headers) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("Start request must be provided");
            }

            executionRegistry.attachStudyWebSocket(headers.getSessionId(), request.sessionId(), studyId);
            RuntimeStudyRequest preparedRequest = executionRegistry.consumePreparedStudy(studyId, request.sessionId());
            runOrchestratorService.startRuntimeStudy(preparedRequest);
        } catch (Exception ex) {
            wsSender.sendToStudy(studyId, RuntimeStudyWsPayload.failed(studyId, ex.getMessage()));
        }
    }
}