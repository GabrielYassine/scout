package dk.dtu.scout.backend.websocket;

import dk.dtu.scout.backend.dto.run.BatchSummaryResponse;
import dk.dtu.scout.backend.dto.run.RunFinalResponse;

import java.util.List;
import java.util.Map;

public record RunWsPayload(
    String type,
    String runId,
    String message,
    Integer runIndex,
    Long seed,
    String searchSpaceId,
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
    String status,
    Double runtimeMs,
    BatchSummaryResponse summary,
    List<RunFinalResponse> completedRuns
) {
    public static RunWsPayload connected(String runId) {
        return status("RUN_CONNECTED", runId, "Run session connected");
    }

    public static RunWsPayload finished(String runId, String searchSpaceId, BatchSummaryResponse summary, List<RunFinalResponse> completedRuns) {
        return new RunWsPayload(
            "RUN_FINISHED",
            runId,
            "Run finished",
            null,
            null,
            searchSpaceId,
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
            summary,
            completedRuns
        );
    }

    public static RunWsPayload failed(String runId, String message) {
        return status("RUN_FAILED", runId, message == null ? "Run failed" : message);
    }

    public static RunWsPayload progress(
        String runId,
        int runIndex,
        long seed,
        String searchSpaceId,
        String problemId,
        long sequenceId,
        MergeOp iterationsMerge,
        MergeOp evaluationsMerge,
        Map<String, MergeOp> seriesMerge,
        Integer iteration,
        Integer evaluation,
        List<Integer> iterations,
        List<Integer> evaluations,
        Map<String, Object> seriesDelta,
        String status,
        Double runtimeMs
    ) {
        return new RunWsPayload(
            "RUN_PROGRESS",
            runId,
            "Run progress update",
            runIndex,
            seed,
            searchSpaceId,
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
            status,
            runtimeMs,
            null,
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
            null,
            null,
            null,
            null,
            null
        );
    }
}