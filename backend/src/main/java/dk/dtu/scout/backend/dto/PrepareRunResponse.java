package dk.dtu.scout.backend.dto;

public record PrepareRunResponse(
        String sessionId,
        String runId
) {}