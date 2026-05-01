/*
* LabPage is the main page for configuring experiments.
* It handles UI state, drag/drop feedback, and navigation to the run page.
*/
import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useDndMonitor } from "@dnd-kit/core";

import LabLeftbar from "@/shared/components/sidebars/LabLeftbar/LabLeftbar.jsx";
import LabRightbar from "@/shared/components/sidebars/LabRightbar/LabRightbar.jsx";
import RunConfigPuzzle from "@/features/lab/components/runConfigPuzzle/RunConfigPuzzle.jsx";
import Selector from "@/features/lab/components/selector/Selector.jsx";

import "@/features/lab/styles/LabPage.css";

import { usePuzzleConfig } from "@/shared/contexts/usePuzzleConfig.js";
import { useRunExecution } from "@/features/run/contexts/useRunExecution.js";
import { prepareRun } from "@/shared/api/run.js";
import {runTemplates, } from "@/features/lab/templates/runTemplates.js";
import { runtimeStudyTemplates, } from "@/features/lab/templates/runtimeStudyTemplates.js";
import { persistSessionId } from "@/features/lab/utils/sessionStorage.js";
import { parseProblemSizes, buildExecutionContext, buildRuntimeStudyRequest, buildRunRequest, } from "@/features/lab/utils/labExecution.js";

const SHARED_DROP_AREA_ID = "shared-drop-area";
const TOAST_DURATION_MS = 3500;

export default function LabPage({
  catalog,
  catalogLoading,
  catalogError,
}) {
  const {
    puzzleConfig,
    params,
    tspInstance,
    vrpInstance,
    setTspInstance,
    setVrpInstance,
    applyTemplateRunRequest,
    applyTemplateRuntimeStudyRequest,
    handleParamChange,
    handleReset,
  } = usePuzzleConfig();

  const { startRunExecution, startRuntimeStudyExecution } = useRunExecution();

  const [hoverInfo, setHoverInfo] = useState(null);
  const [toastMessage, setToastMessage] = useState("");
  const [toastVisible, setToastVisible] = useState(false);
  const [showRemoveDropZone, setShowRemoveDropZone] = useState(false);

  const toastTimeoutRef = useRef(null);
  const navigate = useNavigate();
  const runMode = params.global?.experimentType ?? "run";

  const visibleTemplates = runMode === "runtimeStudy" ? runtimeStudyTemplates : runTemplates;

  // Listen to drag-and-drop events from dnd-kit to determine when to show the remove drop zone overlay.
  useDndMonitor({
    onDragStart(event) {
      // Check whether the dragged item is a piece that is already placed in the puzzle.
      const isPlacedPiece = event.active?.data?.current?.fromIndex != null;
      setShowRemoveDropZone(isPlacedPiece);
    },
    onDragOver(event) {
      // Keep remove zone hidden for selector pieces.
      const isPlacedPiece = event.active?.data?.current?.fromIndex != null;
      if (!isPlacedPiece) {
        setShowRemoveDropZone(false);
        return;
      }
      // Show remove zone when dragging outside the shared drop area.
      setShowRemoveDropZone(event.over?.id !== SHARED_DROP_AREA_ID);
    },
    onDragEnd() {
      // Hide remove zone after drop.
      setShowRemoveDropZone(false);
    },
    onDragCancel() {
      // Hide remove zone if drag is cancelled.
      setShowRemoveDropZone(false);
    },
  });

  // Cleanup when the component unmounts if a toast timer is still running, stop it.
  useEffect(() => {
    return () => {
      if (toastTimeoutRef.current) {
        clearTimeout(toastTimeoutRef.current);
      }
    };
  }, []);

  // Show a temporary toast message
  function showToast(message) {
    setToastMessage(message);
    setToastVisible(true);
    // If an old timeout already exists, clear it first
    if (toastTimeoutRef.current) {
      clearTimeout(toastTimeoutRef.current);
    }
    // Hide toast after a delay
    toastTimeoutRef.current = setTimeout(() => {
      setToastVisible(false);
    }, TOAST_DURATION_MS);
  }

  function getCatalogItem(type, id) {
    if (!catalog || !id) return null;

    const map = {
      searchSpace: catalog.searchSpaces,
      problem: catalog.problems,
      generator: catalog.generators,
      selection: catalog.selectionRules,
      parentSelectionRule: catalog.parentSelectionRules,
      crossover: catalog.crossovers,
      populationModel: catalog.populationModels,
      stopCondition: catalog.stopConditions,
      observer: catalog.observers,
    };

    return (map[type] ?? []).find((x) => x.id === id) ?? null;
  }

  // When the user hovers over a piece in the puzzle or selector, look up its title and description in the catalog.
  function handlePieceHover(type, id) {
    const item = getCatalogItem(type, id);
    if (!item) return;

    setHoverInfo({
      title: item.displayName,
      description: item.description,
    });
  }

  function clearHover() {
    setHoverInfo(null);
  }

  // Main function to handle the "Run" action, which decides whether to start a runtime study or a standard run based on the selected experiment type.
  async function onRun() {
    try {
      const experimentType = params.global?.experimentType ?? "run";
      if (experimentType === "runtimeStudy") {
        await startRuntimeStudy();
        return;
      }

      await startStandardRun();
    } catch (err) {
      showToast(err.message || "Failed to start run");
    }
  }

  // Start a runtime study by building the appropriate request payload and navigating to the run page in runtime study mode.
  async function startRuntimeStudy() {
    const {
      seed,
      existingSessionId,
      problemParams,
      searchSpaceParams,
    } = buildExecutionContext({
      puzzleConfig,
      params,
      tspInstance,
      vrpInstance,
    });

    const problemSizes = parseProblemSizes(params.global?.problemSizes);
    if (problemSizes.length === 0) {
      throw new Error("Please enter at least one valid problem size");
    }

    const draftRuntimeStudyRequest = buildRuntimeStudyRequest({
      studyId: null,
      sessionId: null,
      puzzleConfig,
      params,
      searchSpaceParams,
      problemParams,
      seed,
      problemSizes,
    });

    const prep = await prepareRun({
      sessionId: existingSessionId,
      executionType: "runtimeStudy",
      runtimeStudyRequest: draftRuntimeStudyRequest,
    });

    const sessionId = prep?.sessionId ?? existingSessionId;
    const studyId = prep?.executionId;

    if (!sessionId || !studyId) {
      throw new Error("Backend did not return sessionId and studyId");
    }

    persistSessionId(sessionId);

    const runtimeStudyRequest = buildRuntimeStudyRequest({
      studyId,
      sessionId,
      puzzleConfig,
      params,
      searchSpaceParams,
      problemParams,
      seed,
      problemSizes,
    });

    startRuntimeStudyExecution({
      studyId,
      runtimeStudyRequest,
      puzzleConfig,
      params,
      tspInstance,
      vrpInstance,
    });

    navigate("/run");
  }

  // Start a standard run by preparing the run on the backend, building the appropriate request payload, and navigating to the run page in run mode.
  async function startStandardRun() {
    const {
      seed,
      existingSessionId,
      problemParams,
      searchSpaceParams,
    } = buildExecutionContext({
      puzzleConfig,
      params,
      tspInstance,
      vrpInstance,
    });

    const draftRunRequest = buildRunRequest({
      runId: null,
      sessionId: null,
      puzzleConfig,
      params,
      searchSpaceParams,
      problemParams,
      seed,
    });

    const prep = await prepareRun({
      sessionId: existingSessionId,
      executionType: "run",
      runRequest: draftRunRequest,
    });

    const sessionId = prep?.sessionId ?? existingSessionId;
    const runId = prep?.executionId;

    if (!sessionId || !runId) {
      throw new Error("Backend did not return sessionId and runId");
    }

    persistSessionId(sessionId);

    const runRequest = buildRunRequest({
      runId,
      sessionId,
      puzzleConfig,
      params,
      searchSpaceParams,
      problemParams,
      seed,
    });

    startRunExecution({
      runId,
      runRequest,
      puzzleConfig,
      params,
      tspInstance,
      vrpInstance,
    });

    navigate("/run");
  }

  // Apply the selected template to the current lab configuration
  function onApplyTemplate(templateId) {
    if (!templateId || !catalog) return;

    if (runMode === "runtimeStudy") {
      const template = runtimeStudyTemplates.find((t) => t.id === templateId);
      if (!template) return;

      applyTemplateRuntimeStudyRequest(template.runtimeStudyRequest, catalog);
      return;
    }

    const template = runTemplates.find((t) => t.id === templateId);
    if (!template) return;

    applyTemplateRunRequest(template.runRequest, catalog);
  }

  return (
    <div className="lab-page">
      {showRemoveDropZone && <div className="lab-remove-overlay" />}

      {toastVisible && (
        <div className="lab-toast" role="status" aria-live="polite">
          {toastMessage}
        </div>
      )}

      <LabLeftbar
        puzzleConfig={puzzleConfig}
        params={params}
        onParamChange={handleParamChange}
        onReset={handleReset}
        onRun={onRun}
        catalog={catalog}
        catalogLoading={catalogLoading}
        catalogError={catalogError}
        templates={visibleTemplates}
        onApplyTemplate={onApplyTemplate}
      />

      <div className="lab-page-content">
        <div className="selector-timeline">
          <RunConfigPuzzle
            onPieceHover={handlePieceHover}
            onPieceLeave={clearHover}
          />
        </div>

        <hr className="rounded" />

        <div className="chosen-selector-container">
          <Selector
            catalog={catalog}
            onPieceHover={handlePieceHover}
            onPieceLeave={clearHover}
            puzzleConfig={puzzleConfig}
            params={params}
          />
        </div>
      </div>

      <LabRightbar
        hoverInfo={hoverInfo}
        tspInstance={tspInstance}
        vrpInstance={vrpInstance}
        onTspInstanceChange={setTspInstance}
        onVrpInstanceChange={setVrpInstance}
      />
    </div>
  );
}