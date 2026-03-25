package dk.dtu.scout.backend.websocket;

import dk.dtu.scout.backend.dto.run.BatchRunResponse;

import java.util.Map;

public record RunWsPayload(
    String type,
    String runId,
    String message,
    Integer runIndex,
    Long seed,
    String problemId,
    Integer iteration,
    Integer evaluation,
    Map<String, Object> seriesDelta,
    BatchRunResponse batch
) {
    public static RunWsPayload connected(String runId) {
        return status("RUN_CONNECTED", runId, "Run session connected");
    }

    public static RunWsPayload disconnected(String runId) {
        return status("RUN_DISCONNECTED", runId, "Run session disconnected");
    }

    public static RunWsPayload finished(String runId, BatchRunResponse batch) {
        return new RunWsPayload(
            "RUN_FINISHED",
            runId,
            "Run finished",
            null,
            null,
            null,
            null,
            null,
            null,
            batch
        );
    }

    public static RunWsPayload failed(String runId, String message) {
        return status("RUN_FAILED", runId, message == null ? "Run failed" : message);
    }

    public static RunWsPayload progress(
        String runId,
        int runIndex,
        long seed,
        String problemId,
        int iteration,
        int evaluation,
        Map<String, Object> seriesDelta
    ) {
        return new RunWsPayload(
            "RUN_PROGRESS",
            runId,
            null,
            runIndex,
            seed,
            problemId,
            iteration,
            evaluation,
            seriesDelta,
            null
        );
    }

    private static RunWsPayload status(String type, String runId, String message) {
        return new RunWsPayload(
            type,
            runId,
            message,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }
}