package dk.dtu.scout.backend.dto.request;

/**
 * Request used to prepare a run or runtime study before opening the websocket stream.
 * The backend generates/reuses a sessionId, generates an executionId, injects
 * those ids into the draft request, validates it, and stores it for websocket start.
 * @param sessionId optional existing browser session id
 * @param executionType either "run" or "runtimeStudy"
 * @param runRequest draft run request, required when executionType is "run"
 * @param runtimeStudyRequest draft runtime study request, required when executionType is "runtimeStudy"
 * @author s235257 & s230632
 */
public record PrepareRunRequest(
    String sessionId,
    String executionType,
    RunRequest runRequest,
    RuntimeStudyRequest runtimeStudyRequest
) {}