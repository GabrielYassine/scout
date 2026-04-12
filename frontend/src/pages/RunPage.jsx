import { useState, useEffect, useMemo, useRef } from "react";
import { Client } from "@stomp/stompjs";
import { useLocation, useNavigate } from "react-router-dom";
import LabLeftbar from "@/components/SideBars/LabLeftbar.jsx";
import LabRightbar from "@/components/SideBars/LabRightbar.jsx";
import RunChart from "@/components/Charts/RunChart.jsx";
import RuntimeStudyChart from "@/components/Charts/RuntimeStudyChart.jsx";
import "./RunPage.css";
import "@/components/SideBars/LabLeftbar.css";
import "@/components/SideBars/LabRightbar.css";
import "@/components/SideBars/FormFields.css";
import { useLocalStorageState } from "@/hooks/useLocalStorageState.js";

export default function RunPage({ catalog, catalogLoading, catalogError }) {
  const location = useLocation();
  const navigate = useNavigate();
  const [savedRun, setSavedRun, clearSavedRun] = useLocalStorageState("scout:lastRun", null);

  const locationState = location.state ?? {};
  const hasIncomingExecution =
    Boolean(locationState.runId) ||
    Boolean(locationState.studyId) ||
    locationState.loading === true;

  const restoredRun = hasIncomingExecution ? null : savedRun;

  const pageMode =
     locationState.pageMode ??
     (locationState.runId ? "run" : null) ??
     (locationState.studyId ? "runtimeStudy" : null) ??
     restoredRun?.pageMode ??
     "run";
  const runId = locationState.runId ?? null;
  const studyId = locationState.studyId ?? null;

  const batchResponse = locationState.batch ?? restoredRun?.batch ?? null;
  const initialLoading = locationState.loading ?? restoredRun?.loading ?? false;
  const initialError = locationState.error ?? null;
  const puzzleConfig = locationState.puzzleConfig ?? restoredRun?.puzzleConfig ?? [];
  const runtimeStudyRequest = locationState.runtimeStudyRequest ?? restoredRun?.runtimeStudyRequest ?? null;  const params = locationState.params ?? restoredRun?.params ?? [];
  const initialTspInstance = locationState.tspInstance ?? restoredRun?.tspInstance ?? null;
  const initialVrpInstance = locationState.vrpInstance ?? restoredRun?.vrpInstance ?? null;

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
  const [studyPoints, setStudyPoints] = useState(() => restoredRun?.studyPoints ?? []);
  const [loading, setLoading] = useState(!!initialLoading);
  const [error, setError] = useState(initialError ?? null);
  const [playbackSpeed, setPlaybackSpeed] = useState(50);
  const [visibleCount, setVisibleCount] = useState(1);

  const studyStartedRef = useRef(false);

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

 const [selectedRunKey, setSelectedRunKey] = useState(() => {
   if (restoredRun?.selectedRunKey != null) {
     return restoredRun.selectedRunKey;
   }
   return Object.keys(averageByProblem).length > 0 ? "average" : "0";
 });

 function handleSelectedRunChange(value) {
   setSelectedRunKey(value);
   setSavedRun((prev) =>
     prev
       ? {
           ...prev,
           selectedRunKey: value,
         }
       : prev
   );
 }

  function normalizeSelectedRunKey(selectedKey, averageRuns, batches) {
       const hasAverage = averageRuns.length > 0;
       const numericIndex = Number(selectedKey);
       if (!Number.isInteger(numericIndex) || numericIndex < 0 || numericIndex >= batches.length) {
         return hasAverage ? "average" : "0";
       }
      return String(numericIndex);
  }

  const effectiveSelectedRunKey = normalizeSelectedRunKey(selectedRunKey,averageRuns,batches);

  const selectedBatch =
    effectiveSelectedRunKey === "average" ? null : batches[Number(effectiveSelectedRunKey)];

  const runs =  effectiveSelectedRunKey === "average" ? averageRuns : selectedBatch?.runs ?? [];

  const [tspInstance] = useState(initialTspInstance);
  const [vrpInstance] = useState(initialVrpInstance);

 const runtimeStudyProblemId =
   location.state?.runtimeStudyRequest?.problemId ??
   puzzleConfig?.problem?.[0]?.id ??
   null;

 const currentAnimationLength = useMemo(() => {
   if (pageMode === "runtimeStudy") {
     return studyPoints.length;
   }

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
   }, [pageMode, studyPoints, runs]);
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
    if (pageMode === "run") {
      if (!runId) return;

      const wsUrl = `${window.location.protocol === "https:" ? "wss" : "ws"}://${window.location.host}/ws`;

      const client = new Client({
        brokerURL: wsUrl,
        reconnectDelay: 0,
      });

      const appendSeriesValue = (series, key, value) => {
        if (value === undefined) return series;

        const next = { ...series };
        const list = Array.isArray(next[key]) ? [...next[key]] : [];

        if (key === "tspCities") {
          next[key] = [value];
          return next;
        }

        if (key === "tspTour" || key === "pheromoneHeatmap") {
          list.push(value);
          next[key] = list;
          return next;
        }

        if (Array.isArray(value)) {
          list.push(value);
          next[key] = list;
          return next;
        }

        list.push(value);
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
        const hasSeriesSnapshot =
          seriesValues.length > 0 && seriesValues.every(Array.isArray);

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
        client.subscribe(`/topic/run/${runId}`, (message) => {
          const data = JSON.parse(message.body);

          if (data.type === "RUN_PROGRESS") {
            setLoading(false);
            setBatch((prev) => mergeProgress(prev, data));
            return;
          }

          if (data.type === "RUN_CONNECTED") {
            console.log("Run WebSocket connected", { runId: data.runId, message: data.message });
            return;
          }

          if (data.type === "RUN_FINISHED") {
            console.log("Run WebSocket finished", { runId: data.runId, message: data.message });
            setLoading(false);
            setBatch(normalizeBatch(data.batch ?? null));
            setSavedRun({
                pageMode: "run",
                batch: normalizeBatch(data.batch ?? null),
                studyPoints: [],
                loading: false,
                puzzleConfig,
                params,
                tspInstance,
                vrpInstance,
                selectedRunKey: 0,
                savedAt: Date.now(),
              });
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
            console.log("Run WebSocket disconnected", { runId: data.runId, message: data.message });
            client.deactivate();
          }
        });

        client.publish({
          destination: `/app/run/${runId}/connect`,
          body: JSON.stringify({}),
        });
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
          console.error("Failed to close run WebSocket", e);
        }
      };
    }

    if (pageMode === "runtimeStudy") {
      if (!studyId || !runtimeStudyRequest) return;

      const wsUrl = `${window.location.protocol === "https:" ? "wss" : "ws"}://${window.location.host}/ws`;

      const client = new Client({
        brokerURL: wsUrl,
        reconnectDelay: 0,
      });

      client.onConnect = async () => {
        client.subscribe(`/topic/study/${studyId}`, (message) => {
          const data = JSON.parse(message.body);

          if (data.type === "STUDY_FINISHED") {
          const sortedPoints = [...(data.study?.points ?? [])].sort(
              (a, b) => Number(a.problemSize) - Number(b.problemSize)
           );
            setLoading(false);
            setStudyPoints(sortedPoints);
            setSavedRun({
                pageMode: "runtimeStudy",
                batch: null,
                studyPoints: sortedPoints,
                loading: false,
                puzzleConfig,
                params,
                tspInstance,
                vrpInstance,
                savedAt: Date.now(),
              });
            client.deactivate();
            return;
          }

          if (data.type === "STUDY_FAILED") {
            setLoading(false);
            setError(data.message || "Runtime study failed");
            client.deactivate();
          }
        });

        if (!studyStartedRef.current) {
          studyStartedRef.current = true;
          //TODO: APi call should be in labpage
          try {
            console.log("POST /api/runtime-study payload:", runtimeStudyRequest);

            const res = await fetch("/api/runtime-study", {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify(runtimeStudyRequest),
            });

            if (!res.ok) {
              let message = `Runtime study failed with status ${res.status}`;
              try {
                const data = await res.json();
                if (data?.message) {
                  message = data.message;
                }
              } catch {}
              throw new Error(message);
            }
          } catch (err) {
            setLoading(false);
            setError(err.message || "Failed to start runtime study");
          }
        }
      };

      client.onStompError = (frame) => {
        console.error("Study WebSocket STOMP error", frame.headers["message"], frame.body);
      };

      client.onWebSocketError = (event) => {
        console.error("Study WebSocket transport error", event);
      };

      client.activate();

      return () => {
        try {
          client.deactivate();
        } catch (e) {
          console.error("Failed to close study WebSocket", e);
        }
      };
    }
  }, [pageMode, runId, studyId, runtimeStudyRequest]);

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
        ) : (
          <>
            {!loading && !error && currentAnimationLength > 0 && (
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
            )}

            {pageMode === "runtimeStudy" ? (
              studyPoints.length === 0 ? (
                <div className="run-chart-panel">
                  <div className="run-chart-title">No runtime study data</div>
                  <div>No study points to plot.</div>
                </div>
              ) : (
                <RuntimeStudyChart
                  studyTitle="Runtime Study"
                  problemId={runtimeStudyProblemId}
                  points={studyPoints}
                  visibleCount={visibleCount}
                />
              )
            ) : batches.length === 0 ? (
              <div className="run-chart-panel">
                <div className="run-chart-title">No run data</div>
                <div>No data to plot.</div>
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
                      onChange={(e) => handleSelectedRunChange(e.target.value)}
                    >
                      {averageRuns.length > 0 && <option value="average">Average</option>}
                      {batches.map((batch, idx) => (
                        <option key={idx} value={String(idx)}>
                          Run {batch.runIndex} (Seed: {batch.seed})
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                <div className="run-stack">
                  {runs.map((run, idx) => (
                    <RunChart
                      key={`${effectiveSelectedRunKey}-${idx}`}
                      run={run}
                      runIndex={selectedBatch?.runIndex ?? "average"}
                      visibleCount={visibleCount}
                      bestFitnessBoxPlot={
                        effectiveSelectedRunKey === "average"
                          ? bestFitnessBoxPlotsByProblem[run.problemId] ?? null
                          : null
                      }
                    />
                  ))}
                </div>
              </>
            )}
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
