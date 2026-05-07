package dk.dtu.scout.backend.websocket;

import dk.dtu.scout.backend.service.ExecutionRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Cleans up prepared or active executions when the browser websocket disconnects.
 * @author s235257
 */
@Component
public class WebSocketDisconnectListener {

    private final ExecutionRegistry executionRegistry;

    public WebSocketDisconnectListener(ExecutionRegistry executionRegistry) {
        this.executionRegistry = executionRegistry;
    }

    /**
     * Handles Spring websocket disconnect events.
     * @param event websocket disconnect event
     */
    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        executionRegistry.handleWebSocketDisconnect(event.getSessionId());
    }
}