package dk.dtu.scout.backend.dto.study;

import java.util.List;

public record RuntimeStudyResponse(
        String studyId,
        List<RuntimeStudyPointResponse> points
) {}
