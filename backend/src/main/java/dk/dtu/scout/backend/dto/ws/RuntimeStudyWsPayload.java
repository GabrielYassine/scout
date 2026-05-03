package dk.dtu.scout.backend.dto.ws;

import dk.dtu.scout.backend.dto.run.RuntimeStudyPointResponse;

/**
 * WebSocket payload used for runtime study updates.
 * The payload type tells the frontend whether the study progressed, finished, or failed.
 * @param type the kind of runtime study event
 * @param studyId the ID of the runtime study
 * @param message human-readable status or error message
 * @param point runtime study data point, only present for progress updates
 * @author s235257 & Ahmed
 */
public record RuntimeStudyWsPayload(
    String type,
    String studyId,
    String message,
    RuntimeStudyPointResponse point
) {

    /**
     * Creates a payload containing one completed runtime study point.
     * @param studyId the ID of the runtime study
     * @param point the completed runtime study point
     * @return progress payload
     */
    public static RuntimeStudyWsPayload progress(String studyId, RuntimeStudyPointResponse point) {
        return new RuntimeStudyWsPayload("STUDY_PROGRESS", studyId, "Study progress", point);
    }

    /**
     * Creates a payload marking the runtime study as finished.
     * @param studyId the ID of the runtime study
     * @return finished payload
     */
    public static RuntimeStudyWsPayload finished(String studyId) {
        return new RuntimeStudyWsPayload("STUDY_FINISHED", studyId, "Study finished", null);
    }

    /**
     * Creates a payload marking the runtime study as failed.
     * @param studyId the ID of the runtime study
     * @param message error message describing why the study failed
     * @return failed payload
     */
    public static RuntimeStudyWsPayload failed(String studyId, String message) {
        return new RuntimeStudyWsPayload("STUDY_FAILED", studyId, message, null);
    }
}