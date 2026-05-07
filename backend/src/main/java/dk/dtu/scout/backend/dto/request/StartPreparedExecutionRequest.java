package dk.dtu.scout.backend.dto.request;

/**
 * Small websocket start request for a prepared execution.
 * The execution id comes from the websocket destination path.
 * @param sessionId browser session id that owns the prepared execution
 * @author s235257 & s230632
 */
public record StartPreparedExecutionRequest(
    String sessionId
) {}