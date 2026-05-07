package dk.dtu.scout.backend.websocket;

import dk.dtu.scout.backend.dto.ws.RunWsPayload;
import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.logging.IterationSnapshot;
import dk.dtu.scout.logging.LoggedSeries;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.SeriesMode;
import dk.dtu.scout.observer.Observer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Observer that streams run progress to the frontend through WebSockets.
 * It sends accumulated logged points as delta packages during the run, and a final FINISHED package when the run ends.
 * Updates Frontend each x evaluations or after a certain time interval,
 * to avoid overwhelming the WebSocket connection and frontend with too many updates.
 * @param <S> the solution representation type used by the current run
 * @author s235257 & Ahmed
 */
public class RunProgressObserver<S> implements Observer<S> {

    private static final ConcurrentHashMap<String, AtomicLong> SEQUENCE_BY_STREAM = new ConcurrentHashMap<>();
    private static final long MIN_WS_UPDATE_INTERVAL_NANOS = 50_000_000L;

    private final WsSender wsSender;
    private final String runId;
    private final int runIndex;
    private final long seed;
    private final String searchSpaceId;
    private final String problemId;
    private final int wsUpdateEveryIterations;
    private final long startTimeNanos;

    private final Map<String, Integer> lastSentSeriesSizeByName = new HashMap<>();

    private int lastSentLogIndex = -1;
    private long lastSentTimeNanos = 0L;

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
        this.searchSpaceId = searchSpaceId;
        this.problemId = problemId;
        this.wsUpdateEveryIterations = wsUpdateEveryIterations;
        this.startTimeNanos = System.nanoTime();

        SEQUENCE_BY_STREAM.computeIfAbsent(streamKey(), key -> new AtomicLong(0));
    }

    private String streamKey() {
        return runId + ":" + runIndex + ":" + problemId + ":" + seed;
    }

    private long nextSequenceId() {
        return SEQUENCE_BY_STREAM.computeIfAbsent(streamKey(), key -> new AtomicLong(0)).incrementAndGet();
    }

    @Override
    public void onStep(IterationSnapshot<S> state, RunLog log) {
        if (!shouldSendStep(state, log)) {
            return;
        }

        int toIndex = log.getEvaluations().size() - 1;
        int fromIndex = lastSentLogIndex + 1;

        lastSentLogIndex = toIndex;
        lastSentTimeNanos = System.nanoTime();

        sendProgress(log, fromIndex, toIndex, MergeOp.APPEND, "ONGOING", null);
    }

    @Override
    public void onEnd(IterationSnapshot<S> state, RunLog log) {
        int toIndex = log.getEvaluations().size() - 1;
        double runtimeMs = (System.nanoTime() - startTimeNanos) / 1_000_000.0;

        if (toIndex <= lastSentLogIndex) {
            sendProgress(log, toIndex, toIndex, MergeOp.REPLACE_LAST, "FINISHED", runtimeMs);
            return;
        }

        int fromIndex = lastSentLogIndex + 1;

        lastSentLogIndex = toIndex;
        lastSentTimeNanos = System.nanoTime();

        sendProgress(log, fromIndex, toIndex, MergeOp.APPEND, "FINISHED", runtimeMs);
    }

    private boolean shouldSendStep(IterationSnapshot<S> state, RunLog log) {
        int logIndex = log.getEvaluations().size() - 1;

        boolean isInitialPoint = logIndex == 0;
        boolean matchesIterationInterval = ((state.iteration() + 1) % wsUpdateEveryIterations) == 0;

        if (!isInitialPoint && !matchesIterationInterval) {
            return false;
        }

        long now = System.nanoTime();
        boolean matchesTimeInterval = now - lastSentTimeNanos >= MIN_WS_UPDATE_INTERVAL_NANOS;

        return isInitialPoint || matchesTimeInterval;
    }

    private void sendProgress(
        RunLog log,
        int fromIndex,
        int toIndex,
        MergeOp evaluationsMerge,
        String status,
        Double runtimeMs
    ) {
        List<Integer> evaluations = new ArrayList<>(log.getEvaluations().subList(fromIndex, toIndex + 1));
        Integer evaluation = evaluations.getLast();

        Map<String, Object> seriesDelta = buildSeriesDelta(log);
        Map<String, MergeOp> seriesMerge = buildSeriesMerge(log, seriesDelta);

        wsSender.sendToRun(
            runId,
            RunWsPayload.progress(
                runId,
                runIndex,
                seed,
                searchSpaceId,
                problemId,
                nextSequenceId(),
                evaluationsMerge,
                seriesMerge,
                evaluation,
                evaluations,
                seriesDelta,
                status,
                runtimeMs
            )
        );
    }

    private Map<String, Object> buildSeriesDelta(RunLog log) {
        Map<String, Object> delta = new LinkedHashMap<>();

        for (Map.Entry<String, LoggedSeries<?>> entry : log.getSeries().entrySet()) {
            String seriesName = entry.getKey();
            LoggedSeries<?> series = entry.getValue();
            List<?> values = series.getValues();

            if (series.getMode() == SeriesMode.LATEST_ONLY) {
                delta.put(seriesName, values.getLast());
                lastSentSeriesSizeByName.put(seriesName, values.size());
                continue;
            }

            int alreadySent = lastSentSeriesSizeByName.getOrDefault(seriesName, 0);

            if (alreadySent >= values.size()) {
                continue;
            }

            delta.put(seriesName, new ArrayList<>(values.subList(alreadySent, values.size())));
            lastSentSeriesSizeByName.put(seriesName, values.size());
        }

        return delta;
    }

    private Map<String, MergeOp> buildSeriesMerge(RunLog log, Map<String, Object> seriesDelta) {
        Map<String, MergeOp> mergeOps = new LinkedHashMap<>();

        for (String seriesName : seriesDelta.keySet()) {
            LoggedSeries<?> series = log.getSeries().get(seriesName);
            mergeOps.put(seriesName, seriesMergeOp(series));
        }

        return mergeOps;
    }

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