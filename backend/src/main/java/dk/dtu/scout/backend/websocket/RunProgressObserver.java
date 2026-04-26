package dk.dtu.scout.backend.websocket;

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

public class RunProgressObserver<S> implements Observer<S> {

    private static final ConcurrentHashMap<String, AtomicLong> SEQUENCE_BY_STREAM = new ConcurrentHashMap<>();

    private final WsSender wsSender;
    private final String runId;
    private final int runIndex;
    private final long seed;
    private final String problemId;
    private final String searchSpaceId;
    private final int wsUpdateEveryIterations;
    private final long startTimeNanos;

    private int lastSentLogIndex = -1;

    public RunProgressObserver(
        WsSender wsSender,
        String runId,
        int runIndex,
        long seed,
        String searchSpaceId,
        String problemId,
        int wsUpdateEveryIterations
    ) {
        this.wsSender = wsSender;
        this.runId = runId;
        this.runIndex = runIndex;
        this.seed = seed;
        this.problemId = problemId;
        this.searchSpaceId = searchSpaceId;
        this.wsUpdateEveryIterations = wsUpdateEveryIterations;
        this.startTimeNanos = System.nanoTime();

        String streamKey = streamKey(runId, problemId, seed);
        SEQUENCE_BY_STREAM.computeIfAbsent(streamKey, k -> new AtomicLong(0));
    }

    private static String streamKey(String runId, String problemId, long seed) {
        return runId + ":" + problemId + ":" + seed;
    }

    public static long nextSequenceIdFor(String runId, String problemId, long seed) {
        String streamKey = streamKey(runId, problemId, seed);
        return SEQUENCE_BY_STREAM.computeIfAbsent(streamKey, k -> new AtomicLong(0)).incrementAndGet();
    }

    private long nextSequenceId() {
        return nextSequenceIdFor(runId, problemId, seed);
    }

    private static MergeOp seriesMergeOp(LoggedSeries<?> series) {
        if (series == null) {
            return MergeOp.APPEND;
        }

        SeriesMode mode = series.getMode();
        if (mode == SeriesMode.LATEST_ONLY) {
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

    @Override
    public void onStep(IterationSnapshot<S> state, RunLog log) {
        if (wsUpdateEveryIterations <= 0) return;

        int logIndex = log.getIterations().size() - 1;
        if (logIndex < 0) return;
        if (logIndex <= lastSentLogIndex) return;

        boolean isInitialPoint = logIndex == 0;
        boolean matchesInterval = ((state.iteration() + 1) % wsUpdateEveryIterations) == 0;

        if (!isInitialPoint && !matchesInterval) {
            return;
        }

        lastSentLogIndex = logIndex;

        sendProgress(log, logIndex, MergeOp.APPEND, MergeOp.APPEND, "ONGOING", null, true);
    }

    @Override
    public void onEnd(IterationSnapshot<S> state, RunLog log) {
        int logIndex = log.getIterations().size() - 1;
        if (logIndex < 0) return;

        double runtimeMs = (System.nanoTime() - startTimeNanos) / 1_000_000.0;

        boolean finalPointAlreadySent = logIndex <= lastSentLogIndex;

        if (finalPointAlreadySent) {
            sendProgress(log, logIndex, MergeOp.REPLACE_LAST, MergeOp.REPLACE_LAST, "FINISHED", runtimeMs, false);
            return;
        }

        lastSentLogIndex = logIndex;
        sendProgress(log, logIndex, MergeOp.APPEND, MergeOp.APPEND, "FINISHED", runtimeMs, true);
    }

    private void sendProgress(
        RunLog log,
        int logIndex,
        MergeOp iterationMergeOp,
        MergeOp evaluationMergeOp,
        String status,
        Double runtimeMs,
        boolean includeSeriesDelta
    ) {
        int iteration = log.getIterations().get(logIndex);
        int evaluation = log.getEvaluations().get(logIndex);

        Map<String, Object> seriesDelta = new LinkedHashMap<>();
        Map<String, MergeOp> seriesMerge = new LinkedHashMap<>();

        if (includeSeriesDelta) {
            for (Map.Entry<String, LoggedSeries<?>> entry : log.getSeries().entrySet()) {
                String key = entry.getKey();
                LoggedSeries<?> loggedSeries = entry.getValue();
                if (loggedSeries == null) continue;

                List<?> values = loggedSeries.getValues();
                if (values == null || values.isEmpty()) continue;

                seriesDelta.put(key, values.getLast());
                seriesMerge.put(key, seriesMergeOp(loggedSeries));
            }
        }

        wsSender.sendToRun(
            runId,
            RunWsPayload.progress(
                runId,
                runIndex,
                seed,
                searchSpaceId,
                problemId,
                nextSequenceId(),
                iterationMergeOp,
                evaluationMergeOp,
                seriesMerge,
                iteration,
                evaluation,
                null,
                null,
                seriesDelta,
                status,
                runtimeMs
            )
        );
    }
}