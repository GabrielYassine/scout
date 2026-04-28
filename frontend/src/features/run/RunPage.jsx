/**
 * Layout and orchestration component for run and runtime-study results.
 * Wires page state, websocket hooks, playback controls, sidebars, and content.
 */
import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

import LabLeftbar from "@/shared/components/sidebars/LabLeftbar/LabLeftbar.jsx";
import LabRightbar from "@/shared/components/sidebars/LabRightbar/LabRightbar.jsx";
import RunControls from "@/features/run/components/controls/RunControls.jsx";
import PopulatedRunContent from "@/features/run/components/PopulatedRunContent.jsx";

import "./styles/RunPage.css";

import { computeAnimationLength } from "@/features/run/utils/runData.js";
import { usePlayback } from "@/features/run/hooks/usePlayback.js";
import { useRunPageState } from "@/features/run/hooks/useRunPageState.js";
import { useRunSelection } from "@/features/run/hooks/useRunSelection.js";
import { useRunWebSocket } from "@/features/run/hooks/useRunWebSocket.js";
import { useRuntimeStudyWebSocket } from "@/features/run/hooks/useRuntimeStudyWebSocket.js";

// Default playback speed used when the RunPage first loads.
const INITIAL_SPEED = 1;

// Renders a simple status panel for failed runs or empty result states.
function renderStatusPanel(title, message) {
  return (
    <div className="run-chart-panel">
      <div className="run-chart-title">{title}</div>
      <div>{message}</div>
    </div>
  );
}

export default function RunPage({ catalog, catalogLoading, catalogError }) {
  const navigate = useNavigate();

  // The chart layout starts stacked and can be changed from RunControls.
  const [layoutMode, setLayoutMode] = useState("stack");

  // This hook resolves the initial page state from router state and localStorage,
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
    restoredRun,
    liveExecution,
    batch,
    setBatch,
    studyPoints,
    setStudyPoints,
    loading,
    setLoading,
    error,
    setError,
    setSavedRun,
    studyStatus,
    setStudyStatus,
  } = useRunPageState();

  // This hook derives run selection options and handlers from the current batch and restored run.
  const {
    batches,
    averageRuns,
    bestFitnessBoxPlotsByProblem,
    selectedBatch,
    runs,
    effectiveSelectedRunKey,
    handleSelectedRunChange,
  } = useRunSelection({
    batch,
    restoredRun,
    setSavedRun,
  });

  // Runtime studies vary the problem size for a single problem, so this extracts
  // the problem id used in the study chart title.
  const runtimeStudyProblemId = runtimeStudyRequest?.problemId ?? puzzleConfig?.problem?.[0]?.id ?? null;

  // During live runs this length grows as websocket packets add more logged data.
  // usePlayback animates up to the currently available length and continues when
  // more points arrive.
  const currentAnimationLength = useMemo(
    () => computeAnimationLength({ pageMode, studyPoints, runs }),
    [pageMode, studyPoints, runs]
  );

  // Drives chart playback. visibleCount controls how many currently available
  // data points are shown, while playbackSpeed and resetPlayback are controlled
  // from RunControls.
  const { playbackSpeed, setPlaybackSpeed, visibleCount, resetPlayback } =
    usePlayback({
      length: currentAnimationLength,
      initialSpeed: INITIAL_SPEED,
    });

  // Start the appropriate websocket lifecycle for live executions.
  // Restored saved results do not reconnect.

  // Normal runs stream incremental batch/run updates.
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

  // Runtime studies stream one aggregated study point per problem size.
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

        {loading ? (
          <div className="run-loading">
            <div className="spinner" aria-label="Loading" />
            <div>Preparing run...</div>
          </div>
        ) : error ? (
          renderStatusPanel("Run failed", error)
        ) : pageMode !== "runtimeStudy" && batches.length === 0 ? (
          renderStatusPanel("No run data", "No data to plot.")
        ) : (
          <PopulatedRunContent
            pageMode={pageMode}
            runtimeStudyProblemId={runtimeStudyProblemId}
            studyPoints={studyPoints}
            studyStatus={studyStatus}
            runs={runs}
            selectedBatch={selectedBatch}
            visibleCount={visibleCount}
            effectiveSelectedRunKey={effectiveSelectedRunKey}
            layoutMode={layoutMode}
            tspInstance={tspInstance}
            vrpInstance={vrpInstance}
            bestFitnessBoxPlotsByProblem={bestFitnessBoxPlotsByProblem}
          />
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