package dk.dtu.scout.backend.websocket;

import dk.dtu.scout.backend.dto.study.RuntimeStudyPointResponse;
import dk.dtu.scout.backend.dto.study.RuntimeStudyResponse;

public record RuntimeStudyWsPayload(
        String type,
        String studyId,
        String message,
        RuntimeStudyPointResponse point,
        RuntimeStudyResponse study
) {
    public static RuntimeStudyWsPayload connected(String studyId) {
        return new RuntimeStudyWsPayload("STUDY_CONNECTED", studyId, "Study session connected", null, null);
    }

    public static RuntimeStudyWsPayload finished(String studyId, RuntimeStudyResponse study) {
        return new RuntimeStudyWsPayload("STUDY_FINISHED", studyId, "Study finished", null, study);
    }

    public static RuntimeStudyWsPayload failed(String studyId, String message) {
        return new RuntimeStudyWsPayload("STUDY_FAILED", studyId, message, null, null);
    }
}