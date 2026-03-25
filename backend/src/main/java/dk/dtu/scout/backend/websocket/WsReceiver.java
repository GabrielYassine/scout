package dk.dtu.scout.backend.websocket;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class WsReceiver {

    private final WsSender wsSender;
    private final RunStatusService runStatusService;

    public WsReceiver(WsSender wsSender, RunStatusService runStatusService) {
        this.wsSender = wsSender;
        this.runStatusService = runStatusService;
    }

    @MessageMapping("/run/{runId}/connect")
    public void connect(@DestinationVariable String runId) {
        wsSender.sendToRun(runId, RunWsPayload.connected(runId));
        var finished = runStatusService.getFinishedResponse(runId);
        if (finished != null) {
            wsSender.sendToRun(runId, RunWsPayload.finished(runId, finished));
        } else if (runStatusService.isFinished(runId)) {
            wsSender.sendToRun(runId, RunWsPayload.finished(runId, null));
        }
    }

    @MessageMapping("/run/{runId}/disconnect")
    public void disconnect(@DestinationVariable String runId) {
        wsSender.sendToRun(runId, RunWsPayload.disconnected(runId));
    }
}