package dk.dtu.scout.backend.websocket;

import java.util.Map;

public record RunWsUpdate(
        String type,
        String runId,
        int runIndex,
        long seed,
        String problemId,
        int iteration,
        int evaluation,
        Map<String, Object> seriesDelta
) {}
