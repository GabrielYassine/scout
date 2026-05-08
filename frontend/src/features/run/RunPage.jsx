/**
 * Run page layout with controls and live charts.
 * @author s235257 & s230632
 */
import { useMemo, useState } from "react";

import LabLeftbar from "@/shared/components/sidebars/LabLeftbar/LabLeftbar.jsx";
import LabRightbar from "@/shared/components/sidebars/LabRightbar/LabRightbar.jsx";
import RunControls from "@/features/run/components/controls/RunControls.jsx";
import PopulatedRunContent from "@/features/run/components/PopulatedRunContent.jsx";

import "./styles/RunPage.css";

import { computeAnimationLength } from "@/features/run/utils/runData.js";
import { usePlayback } from "@/features/run/hooks/usePlayback.js";
import { useRunSelection } from "@/features/run/hooks/useRunSelection.js";
import { useRunExecution } from "@/features/run/contexts/useRunExecution.js";

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
  // The chart layout starts stacked and can be changed from RunControls.
  const [layoutMode, setLayoutMode] = useState("stack");

  // Execution state is owned by RunExecutionProvider so websocket connections
  // can survive normal route navigation.
  const {
    pageMode,
    runtimeStudyRequest,
    puzzleConfig,
    params,
    tspInstance,
    vrpInstance,
    restoredRun,
    batch,
    studyPoints,
    loading,
    error,
    setSavedRun,
    studyStatus,
  } = useRunExecution();

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
  const runtimeStudyProblemId =
    runtimeStudyRequest?.problemId ?? puzzleConfig?.problem?.[0]?.id ?? null;

  // During live runs this length grows as websocket packets add more logged data.
  // usePlayback animates up to the currently available length and continues when
  // more points arrive.
  const currentAnimationLength = useMemo(
    () => computeAnimationLength({ pageMode, studyPoints, runs }),
    [pageMode, studyPoints, runs]
  );
  // If the page is reopened from an already existing saved/restored execution,
  // show the full graph immediately instead of replaying it from the beginning.
  const showAllImmediately = !loading &&  !!restoredRun && currentAnimationLength > 0;

  // Drives chart playback. visibleCount controls how many currently available
  // data points are shown, while playbackSpeed and resetPlayback are controlled
  // from RunControls.
  const { playbackSpeed, setPlaybackSpeed, visibleCount, resetPlayback } =
    usePlayback({
      length: currentAnimationLength,
      initialSpeed: INITIAL_SPEED,
      showAllImmediately,
    });

  return (
    <div className="run-page">
      <div className="run-sidebar run-sidebar--disabled" aria-disabled="true">
        <LabLeftbar
          puzzleConfig={puzzleConfig}
          params={params}
          onParamChange={() => {}}
          onReset={undefined}
          onRun={undefined}
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

        {error ? (
          renderStatusPanel("Run failed", error)
        ) : loading && pageMode !== "runtimeStudy" && batches.length === 0 ? (
          renderStatusPanel("Run starting", "Waiting for the first data...")
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