package dk.dtu.scout.backend.dto.request;

/**
 * Response sent to the frontend when a run is prepared, containing the session and execution ids.
 * @author s235257
 */
public record PrepareRunResponse(
    String sessionId,
    String executionId
) {}