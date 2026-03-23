package dk.dtu.scout.backend.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WsSender {

    private final SimpMessagingTemplate template;

    public WsSender(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void sendToRun(String runId, Object payload) {
        template.convertAndSend("/topic/run/" + runId, payload);
    }
}