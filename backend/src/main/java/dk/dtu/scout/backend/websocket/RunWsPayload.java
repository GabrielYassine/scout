package dk.dtu.scout.backend.websocket;

import dk.dtu.scout.backend.dto.run.BatchSummaryResponse;

import java.util.List;
import java.util.Map;

public record RunWsPayload(
    String type,
    String runId,
    String message,
    Integer runIndex,
    Long seed,
    String problemId,
    Long sequenceId,
    MergeOp iterationsMerge,
    MergeOp evaluationsMerge,
    Map<String, MergeOp> seriesMerge,
    Integer iteration,
    Integer evaluation,
    List<Integer> iterations,
    List<Integer> evaluations,
    Map<String, Object> seriesDelta,
    BatchSummaryResponse summary
) {
    public static RunWsPayload connected(String runId) {
        return status("RUN_CONNECTED", runId, "Run session connected");
    }

    public static RunWsPayload finished(String runId, BatchSummaryResponse summary) {
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
            null,
            null,
            null,
            null,
            summary
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
        long sequenceId,
        MergeOp iterationsMerge,
        MergeOp evaluationsMerge,
        Map<String, MergeOp> seriesMerge,
        Integer iteration,
        Integer evaluation,
        List<Integer> iterations,
        List<Integer> evaluations,
        Map<String, Object> seriesDelta
    ) {
        return new RunWsPayload(
            "RUN_PROGRESS",
            runId,
            "Run progress update",
            runIndex,
            seed,
            problemId,
            sequenceId,
            iterationsMerge,
            evaluationsMerge,
            seriesMerge,
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
            null,
            null,
            null,
            null,
            null
        );
    }
}
