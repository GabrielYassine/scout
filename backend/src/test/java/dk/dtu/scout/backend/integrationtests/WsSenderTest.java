package dk.dtu.scout.backend.integrationtests;

import dk.dtu.scout.backend.websocket.WsSender;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WsSenderTest {

    private final SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
    private final WsSender wsSender = new WsSender(template);

    @Test
    void sendToRun_sendsPayloadToRunTopic() {
        Object payload = "run-payload";
        wsSender.sendToRun("run-123", payload);
        verify(template).convertAndSend("/topic/run/run-123", payload);
    }

    @Test
    void sendToStudy_sendsPayloadToStudyTopic() {
        Object payload = "study-payload";
        wsSender.sendToStudy("study-123", payload);
        verify(template).convertAndSend("/topic/study/study-123", payload);
    }
}