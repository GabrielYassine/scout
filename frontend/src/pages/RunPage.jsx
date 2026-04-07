import { useState, useEffect, useMemo } from "react";
import { Client } from "@stomp/stompjs";
import { useLocation, useNavigate } from "react-router-dom";
import LabLeftbar from "../components/SideBars/LabLeftbar.jsx";
import LabRightbar from "../components/SideBars/LabRightbar.jsx";
import RunChart from "../components/charts/RunChart.jsx";
import "./RunPage.css";
import "../components/SideBars/LabLeftbar.css";
import "../components/SideBars/LabRightbar.css";
import "../components/SideBars/FormFields.css";

export default function RunPage({ catalog, catalogLoading, catalogError }) {
  const location = useLocation();
  const navigate = useNavigate();

  const runId = location.state?.runId;

  const batchResponse = location.state?.batch;
  const initialLoading = location.state?.loading;
  const initialError = location.state?.error;
  const puzzleConfig = location.state?.puzzleConfig ?? [];
  const params = location.state?.params ?? [];
  const initialTspInstance = location.state?.tspInstance ?? null;
  const initialVrpInstance = location.state?.vrpInstance ?? null;

  const normalizeSeriesMap = (series) => {
    if (!series) return {};
    return Object.fromEntries(
      Object.entries(series).map(([key, value]) => {
        if (value && typeof value === "object" && Array.isArray(value.values)) {
          return [key, value.values];
        }
        if (Array.isArray(value)) {
          return [key, value];
        }
        return [key, []];
      })
    );
  };

  const normalizeBatch = (incoming) => {
    if (!incoming) return incoming;
    const batches = (incoming.batches ?? []).map((batchItem) => ({
      ...batchItem,
      runs: (batchItem.runs ?? []).map((run) => ({
        ...run,
        series: normalizeSeriesMap(run.series),
      })),
    }));
    return { ...incoming, batches };
  };

  const [batch, setBatch] = useState(() => normalizeBatch(batchResponse ?? null));
  const [loading, setLoading] = useState(!!initialLoading);
  const [error, setError] = useState(initialError ?? null);
  const [playbackSpeed, setPlaybackSpeed] = useState(50);
  const [visibleCount, setVisibleCount] = useState(1);

  const batches = batch?.batches ?? [];
  const averageByProblem = batch?.summary?.averageByProblem ?? {};
  const bestFitnessBoxPlotsByProblem =batch?.summary?.bestFitnessBoxPlotsByProblem ?? {};
 const averageRunTimeByProblem = batch?.summary?.averageRunTimeByProblem ?? {};


  const averageRuns = useMemo(
    () =>
      Object.entries(averageByProblem).map(([problemId, avg]) => ({
        problemId,
        iterations: avg.iterations ?? [],
        evaluations: avg.evaluations ?? [],
        series: avg.series ?? {},
         runtimeMs: averageRunTimeByProblem[problemId] ?? null,
        isAverage: true,
      })),
    [averageByProblem, averageRunTimeByProblem]
  );

  const [selectedRunKey, setSelectedRunKey] = useState(
    Object.keys(averageByProblem).length > 0 ? "average" : "0"
  );

  const effectiveSelectedRunKey =
    selectedRunKey === "average" && averageRuns.length === 0 && batches.length > 0
      ? "0"
      : selectedRunKey;

  const selectedBatch =
    effectiveSelectedRunKey === "average" ? null : batches[Number(effectiveSelectedRunKey)];

  const runs =
    effectiveSelectedRunKey === "average" ? averageRuns : selectedBatch?.runs ?? [];

  const [tspInstance] = useState(initialTspInstance);
  const [vrpInstance] = useState(initialVrpInstance);

   const currentAnimationLength = useMemo(() => {
     if (!runs.length) return 0;

     return Math.max(
       ...runs.map((run) => {
         const series = run?.series ?? {};

         const hypercubeLen = Math.min(
           series.hypercubeX?.length ?? 0,
           series.hypercubeY?.length ?? 0
         );

         const tspLen = series.tspTour?.length ?? 0;

         const normalLens = Object.values(series)
           .filter(Array.isArray)
           .map((arr) => arr.length);

         return Math.max(hypercubeLen, tspLen, ...normalLens, run.evaluations?.length ?? 0);
       })
     );
   }, [runs]);
   useEffect(() => {
     if (!currentAnimationLength) return;

     const stepSize = Math.max(1, Math.floor(playbackSpeed / 15));

     const interval = setInterval(() => {
       setVisibleCount((prev) => {
         if (prev >= currentAnimationLength) return prev;
         return Math.min(prev + stepSize, currentAnimationLength);
       });
     }, 30);

     return () => clearInterval(interval);
   }, [currentAnimationLength, playbackSpeed]);
function handleResetPlayback() {
  setVisibleCount(1);
}

  useEffect(() => {
    if (!runId) {
      console.log("No runId provided, skipping WebSocket connection");
      return;
    }

    const wsUrl = `${window.location.protocol === "https:" ? "wss" : "ws"}://${window.location.host}/ws`;
    console.log("Connecting to WebSocket at", wsUrl, "for runId", runId);

    const client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 0,
    });

    const appendSeriesValue = (series, key, value) => {
      if (value === undefined) return series;
      const next = { ...series };
      if (Array.isArray(value)) {
        const list = Array.isArray(next[key]) ? [...next[key]] : [];
        if (key === "tspCities") {
          if (list.length === 0) list.push(value);
          next[key] = list;
          return next;
        }
        if (key === "pheromoneHeatmap") {
          list.push(value);
          next[key] = list;
          return next;
        }
        next[key] = [...value];
        return next;
      }
      const list = Array.isArray(next[key]) ? [...next[key]] : [];
      if (key === "tspCities") {
        if (list.length === 0) list.push(value);
      } else {
        list.push(value);
      }
      next[key] = list;
      return next;
    };

    const mergeProgress = (prev, update) => {
      const base = prev ?? { runId: update.runId, batches: [], summary: null };
      const nextBatches = [...(base.batches ?? [])];
      const batchIndex = nextBatches.findIndex((b) => b.runIndex === update.runIndex);
      const runGroup =
        batchIndex >= 0
          ? { ...nextBatches[batchIndex] }
          : { runIndex: update.runIndex, seed: update.seed, runs: [] };

      const runsList = [...(runGroup.runs ?? [])];
      const runIndex = runsList.findIndex((r) => r.problemId === update.problemId);
      const run =
        runIndex >= 0
          ? { ...runsList[runIndex] }
          : {
              problemId: update.problemId,
              iterations: [],
              evaluations: [],
              series: {},
              runtimeMs: 0,
              finalEvaluations: 0,
            };

      if (Array.isArray(update.iterations)) {
        run.iterations = [...update.iterations];
      } else if (update.iteration != null) {
        run.iterations = [...run.iterations, update.iteration];
      }

      if (Array.isArray(update.evaluations)) {
        run.evaluations = [...update.evaluations];
      } else if (update.evaluation != null) {
        run.evaluations = [...run.evaluations, update.evaluation];
      }
      const seriesDelta = update.seriesDelta ?? {};
      const seriesValues = Object.values(seriesDelta);
      const hasSeriesSnapshot = seriesValues.length > 0 && seriesValues.every(Array.isArray);
      if (hasSeriesSnapshot) {
        run.series = normalizeSeriesMap(seriesDelta);
      } else {
        run.series = Object.entries(seriesDelta).reduce(
          (acc, [key, value]) => appendSeriesValue(acc, key, value),
          run.series ?? {}
        );
      }

      runsList[runIndex >= 0 ? runIndex : runsList.length] = run;
      runGroup.runs = runsList;
      if (batchIndex >= 0) {
        nextBatches[batchIndex] = runGroup;
      } else {
        nextBatches.push(runGroup);
      }

      return { ...base, batches: nextBatches };
    };

    client.onConnect = () => {
      console.log("WebSocket connected", runId);
      client.subscribe(`/topic/run/${runId}`, (message) => {
        const data = JSON.parse(message.body);

        if (data.type === "RUN_PROGRESS") {
          setLoading(false);
          setBatch((prev) => mergeProgress(prev, data));
          return;
        }

        if (data.type === "RUN_FINISHED") {
          setLoading(false);
          setBatch(normalizeBatch(data.batch ?? null));
          client.deactivate();
          return;
        }

        if (data.type === "RUN_FAILED") {
          setLoading(false);
          setError(data.message || "Run failed");
          client.deactivate();
          return;
        }

        if (data.type === "RUN_DISCONNECTED") {
          client.deactivate();
        }
      });

      client.publish({
        destination: `/app/run/${runId}/connect`,
        body: JSON.stringify({}),
      });
    };

    client.onStompError = (frame) => {
      console.error("WebSocket STOMP error", frame.headers["message"], frame.body);
    };

    client.onWebSocketError = (event) => {
      console.error("WebSocket transport error", event);
    };

    client.activate();

    return () => {
      try {
        if (client.connected) {
          client.publish({
            destination: `/app/run/${runId}/disconnect`,
            body: JSON.stringify({}),
          });
        }
        client.deactivate();
      } catch (e) {
        console.error("Failed to close WebSocket", e);
      }
    };
  }, [runId]);

  return (
    <div className="run-page">
      <div className="run-sidebar run-sidebar--disabled" aria-disabled="true">
        <LabLeftbar
          puzzleConfig={puzzleConfig}
          params={params}
          onParamChange={() => {}}
          onReset={() => navigate("/lab")}
          onRun={() => navigate("/lab")}
          catalog={catalog}
          catalogLoading={catalogLoading}
          catalogError={catalogError}
          readOnly
        />
      </div>

      <div className="run-page-content">
        {loading ? (
          <div className="run-loading">
            <div className="spinner" aria-label="Loading" />
            <div>Preparing run...</div>
          </div>
        ) : error ? (
          <div className="run-chart-panel">
            <div className="run-chart-title">Run failed</div>
            <div>{error}</div>
          </div>
        ) : batches.length === 0 ? (
          <div className="run-chart-panel">
            <div className="run-chart-title">No run data</div>
            <div>No data to plot..</div>
          </div>
        ) : (
          <>
            {(averageRuns.length > 0 || batches.length > 1) && (
              <div className="run-selector">
                <label htmlFor="batch-select" className="field-label">
                  Select Run:
                </label>
                <select
                  id="batch-select"
                  className="field-input"
                  value={effectiveSelectedRunKey}
                  onChange={(e) => setSelectedRunKey(e.target.value)}
                >
                  {averageRuns.length > 0 && (
                    <option value="average">Average</option>
                  )}

                  {batches.map((batch, idx) => (
                    <option key={idx} value={String(idx)}>
                      Run {batch.runIndex} (Seed: {batch.seed})
                    </option>
                  ))}
                </select>
              </div>
            )}

            <div className="run-speed-control">
              <label htmlFor="playback-speed" className="field-label">
                Graph speed:
              </label>
              <input
                id="playback-speed"
                className="field-input"
                type="range"
                min="1"
                max="200"
                value={playbackSpeed}
                onChange={(e) => setPlaybackSpeed(Number(e.target.value))}
              />
              <span>{playbackSpeed}</span>
              <button
                type="button"
                className="reset-playback-button"
                onClick={handleResetPlayback}
              >
                Reset
              </button>
            </div>

            <div className="run-stack">
              {runs.map((run, idx) => (
                <RunChart
                  key={`${selectedRunKey}-${idx}`}
                  run={run}
                  runIndex={selectedBatch?.runIndex ?? "average"}
                  visibleCount={visibleCount}
                  bestFitnessBoxPlot={
                      selectedRunKey === "average"
                        ? bestFitnessBoxPlotsByProblem[run.problemId] ?? null
                        : null
                  }
                />
              ))}
            </div>
          </>
        )}
      </div>
      <div className="run-sidebar run-sidebar--disabled" aria-disabled="true">
        <LabRightbar
          hoverInfo={null}
          tspInstance={tspInstance}
          vrpInstance={vrpInstance}
          onTspInstanceChange={() => {}}
          onVrpInstanceChange={() => {}}
        />
      </div>
    </div>
  );
}