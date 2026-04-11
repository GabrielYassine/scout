package dk.dtu.scout.backend.websocket;

import dk.dtu.scout.backend.dto.run.BatchRunResponse;

import java.util.List;
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
    List<Integer> iterations,
    List<Integer> evaluations,
    Map<String, Object> seriesDelta,
    BatchRunResponse batch
) {
    public static RunWsPayload connected(String runId) {
        System.out.println("Creating connected payload for run " + runId);
        return status("RUN_CONNECTED", runId, "Run session connected");
    }

    public static RunWsPayload disconnected(String runId) {
        System.out.println("Creating disconnected payload for run " + runId);
        return status("RUN_DISCONNECTED", runId, "Run session disconnected");
    }

    public static RunWsPayload finished(String runId, BatchRunResponse batch) {
        System.out.println("Creating finished payload for run " + runId);
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
            null,
            null,
            batch
        );
    }

    public static RunWsPayload failed(String runId, String message) {
        System.out.println("Creating failed payload for run " + runId + " with message: " + message);
        return status("RUN_FAILED", runId, message == null ? "Run failed" : message);
    }

    public static RunWsPayload progress(
        String runId,
        int runIndex,
        long seed,
        String problemId,
        int iteration,
        int evaluation,
        List<Integer> iterations,
        List<Integer> evaluations,
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
            iterations,
            evaluations,
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
            null,
            null,
            null
        );
    }
}