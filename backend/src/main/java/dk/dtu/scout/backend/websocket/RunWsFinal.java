package dk.dtu.scout.backend.websocket;

import dk.dtu.scout.backend.dto.run.BatchRunResponse;

public record RunWsFinal(
        String type,
        String runId,
        BatchRunResponse batch
) {}
