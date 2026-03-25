package dk.dtu.scout.backend.websocket;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.logging.LoggedSeries;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RunProgressObserver<S> implements Observer<S> {

    private final WsSender wsSender;
    private final String runId;
    private final int runIndex;
    private final long seed;
    private final String problemId;
    private final int wsUpdateEveryIterations;

    private int lastSentLogIndex = -1;

    public RunProgressObserver(
        WsSender wsSender,
        String runId,
        int runIndex,
        long seed,
        String problemId,
        int wsUpdateEveryIterations
    ) {
        this.wsSender = wsSender;
        this.runId = runId;
        this.runIndex = runIndex;
        this.seed = seed;
        this.problemId = problemId;
        this.wsUpdateEveryIterations = wsUpdateEveryIterations;
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
    public void onStep(RunState<S> state, RunLog log) {
        if (wsUpdateEveryIterations <= 0) return;
        if ((state.iteration() + 1) % wsUpdateEveryIterations != 0) return;

        int logIndex = log.getIterations().size() - 1;
        if (logIndex <= lastSentLogIndex) return;
        lastSentLogIndex = logIndex;

        int iteration = log.getIterations().get(logIndex);
        int evaluation = log.getEvaluations().get(logIndex);

        Map<String, Object> seriesDelta = new LinkedHashMap<>();
        for (Map.Entry<String, LoggedSeries<?>> entry : log.getSeries().entrySet()) {
            LoggedSeries<?> loggedSeries = entry.getValue();
            if (loggedSeries == null) continue;
            List<?> values = loggedSeries.getValues();
            if (values == null || values.isEmpty()) continue;
            seriesDelta.put(entry.getKey(), values.getLast());
        }

        wsSender.sendToRun(
            runId,
            RunWsPayload.progress(
                runId,
                runIndex,
                seed,
                problemId,
                iteration,
                evaluation,
                null,
                null,
                seriesDelta
            )
        );
    }
}
