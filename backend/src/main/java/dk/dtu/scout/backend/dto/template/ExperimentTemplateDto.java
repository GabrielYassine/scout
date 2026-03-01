package dk.dtu.scout.backend.dto.template;

import java.util.Map;

public record ExperimentTemplateDto(
        String id,
        String displayName,
        String description,
        Map<String, Object> runRequest
) {}