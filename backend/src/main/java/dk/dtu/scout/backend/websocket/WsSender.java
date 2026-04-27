package dk.dtu.scout.backend.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Small wrapper around SimpMessagingTemplate for sending WebSocket messages.
 * Keeps topic construction in one place instead of repeating topic strings across services.
 * @author s235257 & Ahmed
 */
@Service
public class WsSender {

    private final SimpMessagingTemplate template;

    public WsSender(SimpMessagingTemplate template) {
        this.template = template;
    }

    /**
     * Sends a payload to the topic for a normal run.
     * @param runId the run ID used in the topic path
     * @param payload the payload to send
     */
    public void sendToRun(String runId, Object payload) {
        template.convertAndSend("/topic/run/" + runId, payload);
    }

    /**
     * Sends a payload to the topic for a runtime study.
     * @param studyId the study ID used in the topic path
     * @param payload the payload to send
     */
    public void sendToStudy(String studyId, Object payload) {
        template.convertAndSend("/topic/study/" + studyId, payload);
    }
}