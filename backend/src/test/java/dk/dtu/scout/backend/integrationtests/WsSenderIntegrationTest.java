package dk.dtu.scout.backend.integrationtests;

import dk.dtu.scout.backend.websocket.WsSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.verify;

@SpringBootTest
class WsSenderIntegrationTest {

    @Autowired
    private WsSender wsSender;

    @MockBean
    private SimpMessagingTemplate template;

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