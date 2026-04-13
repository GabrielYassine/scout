package dk.dtu.scout.backend.websocket;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class WsReceiver {

    private final WsSender wsSender;

    public WsReceiver(WsSender wsSender) {
        this.wsSender = wsSender;
    }

    @MessageMapping("/run/{runId}/connect")
    public void connect(@DestinationVariable String runId) {
        wsSender.sendToRun(runId, RunWsPayload.connected(runId));
    }

    @MessageMapping("/run/{runId}/disconnect")
    public void disconnect(@DestinationVariable String runId) {
        wsSender.sendToRun(runId, RunWsPayload.disconnected(runId));
    }
}