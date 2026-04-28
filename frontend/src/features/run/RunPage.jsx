/**
 * Layout and orchestration component for run and runtime-study results.
 * Wires page state, websocket hooks, playback controls, sidebars, and content.
 */
import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

import LabLeftbar from "@/shared/components/sidebars/LabLeftbar.jsx";
import LabRightbar from "@/shared/components/sidebars/LabRightbar.jsx";
import RunControls from "@/features/run/components/controls/RunControls.jsx";
import PopulatedRunContent from "@/features/run/components/PopulatedRunContent.jsx";

import "./styles/RunPage.css";

import { computeAnimationLength } from "@/features/run/utils/runData.js";
import { usePlayback } from "@/features/run/hooks/usePlayback.js";
import { useRunPageState } from "@/features/run/hooks/useRunPageState.js";
import { useRunSelection } from "@/features/run/hooks/useRunSelection.js";
import { useRunWebSocket } from "@/features/run/hooks/useRunWebSocket.js";
import { useRuntimeStudyWebSocket } from "@/features/run/hooks/useRuntimeStudyWebSocket.js";

const INITIAL_SPEED = 1;

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
  const [layoutMode, setLayoutMode] = useState("stack");

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

  const runtimeStudyProblemId =
    runtimeStudyRequest?.problemId ?? puzzleConfig?.problem?.[0]?.id ?? null;

  const currentAnimationLength = useMemo(
    () => computeAnimationLength({ pageMode, studyPoints, runs }),
    [pageMode, studyPoints, runs]
  );

  const { playbackSpeed, setPlaybackSpeed, visibleCount, resetPlayback } =
    usePlayback({
      length: currentAnimationLength,
      initialSpeed: INITIAL_SPEED,
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