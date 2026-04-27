package dk.dtu.scout.backend.dto.request;

/**
 * Request to prepare a run by creating a session or reusing an existing session.
 * @param sessionId an optional session ID to reuse, or null to create a new session.
 * @author s235257
 */
public record PrepareRunRequest(
    String sessionId
) {}