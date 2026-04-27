package dk.dtu.scout.backend.websocket;

import dk.dtu.scout.backend.dto.run.BatchSummaryResponse;

import java.util.List;
import java.util.Map;

/**
 * WebSocket payload used for normal run updates.
 * The same payload type is used for connection, progress, finished, and failed events.
 * For RUN_PROGRESS events, the payload contains a small delta update for one run/problem stream.
 * For RUN_FINISHED events, the payload contains the final batch summary.
 * @param type the kind of run event
 * @param runId the ID of the run
 * @param message human-readable status message
 * @param runIndex index of the repeated runtime, used for progress events
 * @param seed seed used for the repeated runtime
 * @param searchSpaceId ID of the selected search space
 * @param problemId ID of the problem being updated
 * @param sequenceId increasing sequence number for ordering progress packets
 * @param iterationsMerge merge operation for iteration data
 * @param evaluationsMerge merge operation for evaluation data
 * @param seriesMerge merge operations for individual series
 * @param iteration latest iteration value for progress updates
 * @param evaluation latest evaluation value for progress updates
 * @param iterations optional full iteration list
 * @param evaluations optional full evaluation list
 * @param seriesDelta latest series values sent as a delta
 * @param status per-run status, such as ONGOING or FINISHED
 * @param runtimeMs runtime in milliseconds for a finished run/problem stream
 * @param summary final batch summary, only present for RUN_FINISHED
 * @author s235257 & Ahmed
 */
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
    BatchSummaryResponse summary
) {
    /**
     * Creates a payload confirming that the frontend is connected to the run stream.
     * @param runId the ID of the run
     * @return connected payload
     */
    public static RunWsPayload connected(String runId) {
        return status("RUN_CONNECTED", runId, "Run session connected");
    }

    /**
     * Creates a payload marking the whole batch run as finished.
     * @param runId the ID of the run
     * @param searchSpaceId ID of the selected search space
     * @param summary final batch summary
     * @return finished payload
     */
    public static RunWsPayload finished(String runId, String searchSpaceId, BatchSummaryResponse summary) {
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
            summary
        );
    }

    /**
     * Creates a payload marking the run as failed.
     * @param runId the ID of the run
     * @param message error message describing why the run failed
     * @return failed payload
     */
    public static RunWsPayload failed(String runId, String message) {
        return status("RUN_FAILED", runId, message == null ? "Run failed" : message);
    }

    /**
     * Creates a progress payload for one run/problem stream.
     * @param runId the ID of the run
     * @param runIndex index of the repeated runtime
     * @param seed seed used for this runtime
     * @param searchSpaceId ID of the selected search space
     * @param problemId ID of the problem being updated
     * @param sequenceId increasing sequence number for ordering progress packets
     * @param iterationsMerge merge operation for iteration data
     * @param evaluationsMerge merge operation for evaluation data
     * @param seriesMerge merge operations for individual series
     * @param iteration latest iteration value
     * @param evaluation latest evaluation value
     * @param iterations optional full iteration list
     * @param evaluations optional full evaluation list
     * @param seriesDelta latest series values sent as a delta
     * @param status per-run status, such as ONGOING or FINISHED
     * @param runtimeMs runtime in milliseconds, usually only present when finished
     * @return progress payload
     */
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
            null
        );
    }
}