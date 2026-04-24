package dk.dtu.scout.backend.websocket;

import dk.dtu.scout.backend.dto.study.RuntimeStudyPointResponse;

public record RuntimeStudyWsPayload(
        String type,
        String studyId,
        String message,
        RuntimeStudyPointResponse point
) {
    public static RuntimeStudyWsPayload connected(String studyId) {
        return new RuntimeStudyWsPayload("STUDY_CONNECTED", studyId, "Study session connected", null);
    }

    public static RuntimeStudyWsPayload progress(String studyId, RuntimeStudyPointResponse point) {
        return new RuntimeStudyWsPayload("STUDY_PROGRESS", studyId, "Study progress", point );
    }
    public static RuntimeStudyWsPayload finished(String studyId) {
        return new RuntimeStudyWsPayload("STUDY_FINISHED", studyId, "Study finished", null);
    }

    public static RuntimeStudyWsPayload failed(String studyId, String message) {
        return new RuntimeStudyWsPayload("STUDY_FAILED", studyId, message, null);
    }
}