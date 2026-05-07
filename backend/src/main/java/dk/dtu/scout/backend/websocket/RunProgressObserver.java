package dk.dtu.scout.backend.websocket;

import dk.dtu.scout.backend.dto.ws.RunWsPayload;
import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.logging.IterationSnapshot;
import dk.dtu.scout.logging.LoggedSeries;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.SeriesMode;
import dk.dtu.scout.observer.Observer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Observer that streams run progress to the frontend through WebSockets.
 * It sends small delta packages during the run, and a final FINISHED package when the run ends.
 * @param <S> the solution representation type used by the current run
 * @author s235257 & Ahmed
 */
public class RunProgressObserver<S> implements Observer<S> {

    private static final ConcurrentHashMap<String, AtomicLong> SEQUENCE_BY_STREAM = new ConcurrentHashMap<>();

    private final WsSender wsSender;
    private final String runId;
    private final int runIndex;
    private final long seed;
    private final String searchSpaceId;
    private final String problemId;
    private final int wsUpdateEveryEvaluations;
    private final long startTimeNanos;
    private int lastSentLogIndex = -1;
    private int lastSentEvaluation = -1;

    public RunProgressObserver(
        WsSender wsSender,
        String runId,
        int runIndex,
        long seed,
        String searchSpaceId,
        String problemId,
        int wsUpdateEveryEvaluations
    ) {
        this.wsSender = wsSender;
        this.runId = runId;
        this.runIndex = runIndex;
        this.seed = seed;
        this.searchSpaceId = searchSpaceId;
        this.problemId = problemId;
        this.wsUpdateEveryEvaluations = wsUpdateEveryEvaluations;
        this.startTimeNanos = System.nanoTime();

        SEQUENCE_BY_STREAM.computeIfAbsent(streamKey(), key -> new AtomicLong(0));
    }

    private String streamKey() {
        return runId + ":" + problemId + ":" + seed;
    }

    private long nextSequenceId() {
        return SEQUENCE_BY_STREAM.computeIfAbsent(streamKey(), key -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Sends an ONGOING progress update when the current log point should be streamed.
     * The first log point is always sent, and later points are sent according to the configured cadence.
     * @param state current iteration snapshot
     * @param log current run log
     */
    @Override
    public void onStep(IterationSnapshot<S> state, RunLog log) {
        if (!shouldSendStep(state, log)) {
            return;
        }

        int logIndex = log.getEvaluations().size() - 1;
        lastSentLogIndex = logIndex;
        lastSentEvaluation = log.getEvaluations().get(logIndex);

        sendProgress(log, logIndex, MergeOp.APPEND, "ONGOING", null, true);
    }

    /**
     * Sends the final FINISHED progress update.
     * If the final log point was already sent during onStep, it replaces the last point instead of appending a duplicate.
     * @param state final iteration snapshot
     * @param log completed run log
     */
    @Override
    public void onEnd(IterationSnapshot<S> state, RunLog log) {
        int logIndex = log.getEvaluations().size() - 1;

        double runtimeMs = (System.nanoTime() - startTimeNanos) / 1_000_000.0;

        if (logIndex <= lastSentLogIndex) {
            sendProgress(log, logIndex, MergeOp.REPLACE_LAST, "FINISHED", runtimeMs, false);
            return;
        }

        lastSentLogIndex = logIndex;
        sendProgress(log, logIndex, MergeOp.APPEND, "FINISHED", runtimeMs, true);
    }

    private boolean shouldSendStep(IterationSnapshot<S> state, RunLog log) {
        int logIndex = log.getEvaluations().size() - 1;
        int evaluation = log.getEvaluations().get(logIndex);


        boolean isInitialPoint = logIndex == 0;
        boolean matchesInterval = lastSentEvaluation < 0 || evaluation - lastSentEvaluation >= wsUpdateEveryEvaluations;
        return isInitialPoint || matchesInterval;
    }

    private void sendProgress(
        RunLog log,
        int logIndex,
        MergeOp axisMergeOp,
        String status,
        Double runtimeMs,
        boolean includeSeriesDelta
    ) {
        int evaluation = log.getEvaluations().get(logIndex);

        Map<String, Object> seriesDelta = includeSeriesDelta ? buildSeriesDelta(log) : Map.of();
        Map<String, MergeOp> seriesMerge = includeSeriesDelta ? buildSeriesMerge(log) : Map.of();

        wsSender.sendToRun(
            runId,
            RunWsPayload.progress(
                runId,
                runIndex,
                seed,
                searchSpaceId,
                problemId,
                nextSequenceId(),
                axisMergeOp,
                seriesMerge,
                evaluation,
                null,
                seriesDelta,
                status,
                runtimeMs
            )
        );
    }

    /**
     * Extracts the newest value from each logged series.
     * This keeps WebSocket packets small because only the new delta is sent.
     *
     * @param log current run log
     * @return map from series name to latest value
     */
    private Map<String, Object> buildSeriesDelta(RunLog log) {
        Map<String, Object> delta = new LinkedHashMap<>();

        for (Map.Entry<String, LoggedSeries<?>> entry : log.getSeries().entrySet()) {
            LoggedSeries<?> series = entry.getValue();
            delta.put(entry.getKey(), series.getValues().getLast());
        }

        return delta;
    }

    /**
     * Builds merge instructions for each series in the delta package.
     *
     * @param log current run log
     * @return map from series name to merge operation
     */
    private Map<String, MergeOp> buildSeriesMerge(RunLog log) {
        Map<String, MergeOp> mergeOps = new LinkedHashMap<>();

        for (Map.Entry<String, LoggedSeries<?>> entry : log.getSeries().entrySet()) {
            LoggedSeries<?> series = entry.getValue();
            mergeOps.put(entry.getKey(), seriesMergeOp(series));
        }

        return mergeOps;
    }

    /**
     * Determines how the frontend should merge a series value.
     * Normal series are appended, while latest-only series replace their previous value.
     * @param series the logged series to inspect
     * @return merge operation for the series
     */
    private static MergeOp seriesMergeOp(LoggedSeries<?> series) {
        if (series.getMode() == SeriesMode.LATEST_ONLY) {
            return MergeOp.REPLACE_LAST;
        }
        return MergeOp.APPEND;
    }

    @Override
    public String id() {
        return "ws-progress";
    }

    @Override
    public String displayName() {
        return "WebSocket progress";
    }

    @Override
    public String description() {
        return "Emits websocket progress updates for the current run.";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }
}