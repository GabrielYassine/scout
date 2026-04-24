import { useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";

import LabLeftbar from "@/shared/components/sidebars/LabLeftbar.jsx";
import LabRightbar from "@/shared/components/sidebars/LabRightbar.jsx";
import RunChart from "@/features/run/components/charts/RunChart.jsx";
import RuntimeStudyChart from "@/features/run/components/charts/RuntimeStudyChart.jsx";
import RunControls from "@/features/run/components/runcontrols/RunControls.jsx";

import "./RunPage.css";

import { useLocalStorageState } from "@/shared/hooks/useLocalStorageState.js";
import {
  computeAnimationLength,
  normalizeBatch,
  normalizeSelectedRunKey,
} from "@/features/run/utils/runData.js";
import { usePlayback } from "@/features/run/hooks/usePlayback.js";
import { useRunWebSocket } from "@/features/run/hooks/useRunWebSocket.js";
import { useRuntimeStudyWebSocket } from "@/features/run/hooks/useRuntimeStudyWebSocket.js";

function resolveRunPageState(locationState, savedRun) {
  const incomingRunId =
    locationState.runId ?? locationState.runRequest?.runId ?? null;
  const incomingStudyId =
    locationState.studyId ?? locationState.runtimeStudyRequest?.studyId ?? null;

  const savedMatchesIncomingRun =
    savedRun?.pageMode === "run" &&
    savedRun?.loading === false &&
    !!incomingRunId &&
    savedRun?.runId === incomingRunId;

  const savedMatchesIncomingStudy =
    savedRun?.pageMode === "runtimeStudy" &&
    savedRun?.loading === false &&
    !!incomingStudyId &&
    savedRun?.studyId === incomingStudyId;

  const shouldIgnoreIncomingState =
    locationState.loading === true &&
    (savedMatchesIncomingRun || savedMatchesIncomingStudy);

  const hasIncomingExecution =
    !shouldIgnoreIncomingState &&
    (Boolean(locationState.runId) ||
      Boolean(locationState.studyId) ||
      locationState.loading === true);

  const restoredRun = hasIncomingExecution ? null : savedRun;

  const pageMode =
    locationState.pageMode ??
    (locationState.runId ? "run" : null) ??
    (locationState.studyId ? "runtimeStudy" : null) ??
    restoredRun?.pageMode ??
    "run";

  const runRequest = locationState.runRequest ?? restoredRun?.runRequest ?? null;
  const runId = locationState.runId ?? runRequest?.runId ?? restoredRun?.runId ?? null;
  const studyId = locationState.studyId ?? restoredRun?.studyId ?? null;

  return {
    pageMode,
    runId,
    studyId,
    runRequest,
    runtimeStudyRequest:
      locationState.runtimeStudyRequest ?? restoredRun?.runtimeStudyRequest ?? null,
    puzzleConfig: locationState.puzzleConfig ?? restoredRun?.puzzleConfig ?? [],
    params: locationState.params ?? restoredRun?.params ?? [],
    tspInstance: locationState.tspInstance ?? restoredRun?.tspInstance ?? null,
    vrpInstance: locationState.vrpInstance ?? restoredRun?.vrpInstance ?? null,
    batchResponse: locationState.batch ?? restoredRun?.batch ?? null,
    studyPoints: restoredRun?.studyPoints ?? [],
    loading: shouldIgnoreIncomingState
      ? restoredRun?.loading ?? false
      : locationState.loading ?? restoredRun?.loading ?? false,
    error: locationState.error ?? restoredRun?.error ?? null,
    restoredRun,
    liveExecution: hasIncomingExecution,
  };
}

function buildAverageRuns(averageByProblem, averageRunTimeByProblem, searchSpaceId) {
  return Object.entries(averageByProblem).map(([problemId, avg]) => ({
    problemId,
    searchSpaceId,
    iterations: avg.iterations ?? [],
    evaluations: avg.evaluations ?? [],
    series: avg.series ?? {},
    runtimeMs: averageRunTimeByProblem[problemId] ?? null,
    isAverage: true,
  }));
}

function renderNoDataPanel(title, message) {
  return (
    <div className="run-chart-panel">
      <div className="run-chart-title">{title}</div>
      <div>{message}</div>
    </div>
  );
}

export default function RunPage({ catalog, catalogLoading, catalogError }) {
  const location = useLocation();
  const navigate = useNavigate();
  const [savedRun, setSavedRun] = useLocalStorageState("scout:lastRun", null);

  const locationState = location.state ?? {};
  const resolvedState = useMemo(
    () => resolveRunPageState(locationState, savedRun),
    [locationState, savedRun]
  );

  const {
    pageMode,
    runId,
    studyId,
    runRequest,
    runtimeStudyRequest,
    puzzleConfig,
    params,
    tspInstance,
    vrpInstance,
    batchResponse,
    studyPoints: initialStudyPoints,
    loading: initialLoading,
    error: initialError,
    restoredRun,
    liveExecution,
  } = resolvedState;

  const [batch, setBatch] = useState(() => normalizeBatch(batchResponse ?? null));
  const [studyPoints, setStudyPoints] = useState(() => initialStudyPoints);
  const [loading, setLoading] = useState(!!initialLoading);
  const [error, setError] = useState(initialError ?? null);
  const [layoutMode, setLayoutMode] = useState("stack");

  const batches = useMemo(
    () => [...(batch?.batches ?? [])].sort((a, b) => a.runIndex - b.runIndex),
    [batch]
  );

  const [studyStatus, setStudyStatus] = useState(() => {
    if (pageMode !== "runtimeStudy") return null;
    if (initialError) return "FAILED";
    if (initialLoading) return "ONGOING";
    return "FINISHED";
  });
  const averageByProblem = batch?.summary?.averageByProblem ?? {};
  const bestFitnessBoxPlotsByProblem = batch?.summary?.bestFitnessBoxPlotsByProblem ?? {};
  const averageRunTimeByProblem = batch?.summary?.averageRunTimeByProblem ?? {};

  const averageRuns = useMemo(
    () => buildAverageRuns(averageByProblem, averageRunTimeByProblem, batch?.searchSpaceId),
    [averageByProblem, averageRunTimeByProblem,batch]
  );

  const [selectedRunKey, setSelectedRunKey] = useState(() => {
    if (restoredRun?.selectedRunKey != null) {
      return restoredRun.selectedRunKey;
    }
    return Object.keys(averageByProblem).length > 0 ? "average" : null;
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

  const effectiveSelectedRunKey = normalizeSelectedRunKey(
    selectedRunKey,
    averageRuns,
    batches
  );

  const selectedBatch =
    effectiveSelectedRunKey === "average"
      ? null
      : batches.find((batchItem) => String(batchItem.runIndex) === String(effectiveSelectedRunKey)) ??
        null;

  const runs =
    effectiveSelectedRunKey === "average"
      ? averageRuns
      : selectedBatch?.runs ?? [];

  const runtimeStudyProblemId =
    runtimeStudyRequest?.problemId ?? puzzleConfig?.problem?.[0]?.id ?? null;

  const currentAnimationLength = useMemo(
    () => computeAnimationLength({ pageMode, studyPoints, runs }),
    [pageMode, studyPoints, runs]
  );

  const { playbackSpeed, setPlaybackSpeed, visibleCount, resetPlayback } = usePlayback({
    length: currentAnimationLength,
    initialSpeed: 50,
  });

  useRunWebSocket({
    enabled: liveExecution && pageMode === "run",
    runId,
    runRequest,
    puzzleConfig,
    params,
    tspInstance,
    vrpInstance,
    setLoading,
    setError,
    setBatch,
    setSavedRun,
  });

  useRuntimeStudyWebSocket({
    enabled: liveExecution && pageMode === "runtimeStudy",
    studyId,
    runtimeStudyRequest,
    puzzleConfig,
    params,
    tspInstance,
    vrpInstance,
    setLoading,
    setError,
    setStudyPoints,
    setSavedRun,
    setStudyStatus,
  });

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
          renderNoDataPanel("Run failed", error)
        ) : pageMode === "runtimeStudy" ?(
            <RuntimeStudyChart
              studyTitle="Runtime Study"
              problemId={runtimeStudyProblemId}
              points={studyPoints}
              visibleCount={visibleCount}
              studyStatus={studyStatus}
            />
        ) : batches.length === 0 ? (
          renderNoDataPanel("No run data", "No data to plot.")
        ) : (
          <>
            <RunControls
              currentAnimationLength={currentAnimationLength}
              playbackSpeed={playbackSpeed}
              setPlaybackSpeed={setPlaybackSpeed}
              resetPlayback={resetPlayback}
              averageRuns={averageRuns}
              batches={batches}
              effectiveSelectedRunKey={effectiveSelectedRunKey}
              onSelectedRunChange={handleSelectedRunChange}
              layoutMode={layoutMode}
              setLayoutMode={setLayoutMode}
            />

            <div
              className={`run-stack ${
                layoutMode === "grid" ? "run-stack--grid" : "run-stack--stack"
              }`}
            >
              {runs.map((run, idx) => (
                <RunChart
                  key={`${effectiveSelectedRunKey}-${idx}`}
                  run={run}
                  runIndex={selectedBatch?.runIndex ?? "average"}
                  visibleCount={visibleCount}
                  instanceName={
                    run.problemId === "tsp"
                      ? tspInstance?.name ?? null
                      : run.problemId === "vrp"
                        ? vrpInstance?.name ?? null
                        : null
                  }
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