// LabPage is the main page for configuring experiments.
// It handles puzzle-piece selection, instance validation, request building,
// and navigation to the run page.
import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useDndMonitor } from "@dnd-kit/core";

import LabLeftbar from "@/shared/components/sidebars/LabLeftbar.jsx";
import LabRightbar from "@/shared/components/sidebars/LabRightbar.jsx";
import RunConfigPuzzle from "@/features/lab/components/runConfigPuzzle/RunConfigPuzzle.jsx";
import Selector from "@/features/lab/components/selector/Selector.jsx";

import "./LabPage.css";

import { usePuzzleConfig } from "@/shared/contexts/usePuzzleConfig.js";
import { useLocalStorageState } from "@/shared/hooks/useLocalStorageState.js";
import { prepareRun } from "@/shared/api/run.js";
import { runTemplates } from "@/features/lab/templates/runTemplates.js";

const SESSION_STORAGE_KEY = "scout:sessionId";
const SHARED_DROP_AREA_ID = "shared-drop-area";
const TOAST_DURATION_MS = 3500;

// generates a unique ID using crypto.randomUUID.
function generateId() {
  return window.crypto?.randomUUID
    ? window.crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}
// Parse a comma-separated string into an array of valid positive integers.
function parseProblemSizes(text) {
  return String(text ?? "")
    .split(",")
    .map((s) => Number(s.trim()))
    .filter((n) => Number.isInteger(n) && n > 0);
}

function getProblemFlags(problemList) {
  return {
    isTspProblem: problemList.some((p) => p.id === "tsp"),
    isVrpProblem: problemList.some((p) => p.id === "vrp"),
  };
}

// Validate that required problem instances, vrp or tsp, exist before starting a run.
function validateProblemInstances({ problemList, tspInstance, vrpInstance }) {
  // Check whether the selected problems include TSP and/or VRP.
  const { isTspProblem, isVrpProblem } = getProblemFlags(problemList);
  // A valid TSP instance must have a non-empty cities array.
  const hasValidTspInstance =
    Array.isArray(tspInstance?.cities) && tspInstance.cities.length > 0;

  // A valid VRP instance must have:
  // - a non-empty customers array
  // - a depot defined
  const hasValidVrpInstance =
    Array.isArray(vrpInstance?.customers) &&
    vrpInstance.customers.length > 0 &&
    vrpInstance?.depot != null;

  if (isTspProblem && !hasValidTspInstance) {
    throw new Error("Please upload or create a TSP instance before running a TSP problem.");
  }

  if (isVrpProblem && !hasValidVrpInstance) {
    throw new Error("Please upload or create a VRP instance before running a VRP problem.");
  }

  return { isTspProblem, isVrpProblem };
}

// Build the problemParams object to be sent to the backend, including TSP or VRP instances if applicable.
function buildProblemParams({ baseProblemParams, tspInstance, vrpInstance, isTspProblem, isVrpProblem }) {
  const problemParams = { ...baseProblemParams };

  if (isTspProblem) {
    problemParams.tspInstance = tspInstance;
  }

  if (isVrpProblem) {
    problemParams.vrpInstance = vrpInstance;
  }

  return problemParams;
}
// Build the searchSpaceParams object to be sent to the backend, including problem size parameters derived from TSP or VRP instances if applicable.
function buildSearchSpaceParams({ baseSearchSpaceParams, tspInstance, vrpInstance, isTspProblem, isVrpProblem, }) {
  const searchSpaceParams = { ...baseSearchSpaceParams };

  if (isTspProblem) {
    searchSpaceParams.n = tspInstance.cities.length;
  }

  if (isVrpProblem) {
    searchSpaceParams.n = vrpInstance.customers.length;
  }

  return searchSpaceParams;
}
// Retrieve the existing session ID from sessionStorage, or return null if it doesn't exist.
function getExistingSessionId() {
  return window.sessionStorage?.getItem(SESSION_STORAGE_KEY) ?? null;
}
// Persist the given session ID to sessionStorage for future runs.
function persistSessionId(sessionId) {
  try {
    window.sessionStorage?.setItem(SESSION_STORAGE_KEY, sessionId);
  } catch {
    // ignore storage errors
  }
}
// Ensure that a session ID exists by returning the existing one or generating and persisting a new one if it doesn't exist.
function ensureSessionId(existingSessionId) {
  if (existingSessionId) return existingSessionId;

  const next = generateId();
  persistSessionId(next);
  return next;
}
// Build the request payload for starting a runtime study.
function buildRuntimeStudyRequest({
  studyId,
  sessionId,
  puzzleConfig,
  params,
  searchSpaceParams,
  problemParams,
  seed,
  problemSizes,
}) {
  return {
    studyId,
    sessionId,
    searchSpaceId: puzzleConfig.searchSpace?.[0]?.id ?? null,
    searchSpaceParams,
    problemId: puzzleConfig.problem?.[0]?.id ?? null,
    problemParams,
    generatorId: puzzleConfig.generator?.[0]?.id ?? null,
    generatorParams: params.generator,
    selectionRuleId: puzzleConfig.selection?.[0]?.id ?? null,
    selectionRuleParams: params.selection,
    populationModelId: puzzleConfig.populationModel?.[0]?.id ?? null,
    populationModelParams: params.populationModel,
    parentSelectionRuleId: puzzleConfig.parentSelectionRule?.[0]?.id ?? null,
    parentSelectionRuleParams: params.parentSelectionRule,
    crossoverId: puzzleConfig.crossover?.[0]?.id ?? null,
    crossoverParams: params.crossover,
    stopConditionIds: puzzleConfig.stopCondition?.map((x) => x.id) ?? [],
    stopConditionParams: params.stopCondition,
    seed,
    problemSizes,
    repetitionsPerSize: params.global?.repetitionsPerSize ?? 30,
    wsUpdateEverySizes: params.global?.wsUpdateEverySizes ?? 1,
  };
}
// Build the request payload for starting a standard run.
function buildRunRequest({
  runId,
  sessionId,
  puzzleConfig,
  params,
  searchSpaceParams,
  problemParams,
  seed,
}) {
  return {
    searchSpaceId: puzzleConfig.searchSpace?.[0]?.id ?? null,
    searchSpaceParams,
    problemIds: puzzleConfig.problem?.map((x) => x.id) ?? [],
    problemParams,
    generatorId: puzzleConfig.generator?.[0]?.id ?? null,
    generatorParams: params.generator,
    selectionRuleId: puzzleConfig.selection?.[0]?.id ?? null,
    selectionRuleParams: params.selection,
    populationModelId: puzzleConfig.populationModel?.[0]?.id ?? null,
    populationModelParams: params.populationModel,
    parentSelectionRuleId: puzzleConfig.parentSelectionRule?.[0]?.id ?? null,
    parentSelectionRuleParams: params.parentSelectionRule,
    crossoverId: puzzleConfig.crossover?.[0]?.id ?? null,
    crossoverParams: params.crossover,
    stopConditionIds: puzzleConfig.stopCondition?.map((x) => x.id) ?? [],
    stopConditionParams: params.stopCondition,
    observerIds: puzzleConfig.observer?.map((x) => x.id) ?? [],
    observerParams: params.observer,
    seed,
    runTimes: params.global?.runTimes ?? 1,
    sessionId,
    runId,
    logEveryIterations: params.global?.logEveryIterations ?? 100,
    wsUpdateEveryIterations: params.global?.wsUpdateEveryIterations ?? 100,
  };
}

export default function LabPage({
  catalog,
  catalogLoading,
  catalogError,
}) {
// Get shared lab state and update functions from the puzzle config context
  const {
    puzzleConfig,
    params,
    tspInstance,
    vrpInstance,
    setTspInstance,
    setVrpInstance,
    applyTemplateRunRequest,
    handleParamChange,
    handleReset,
  } = usePuzzleConfig();

  const [hoverInfo, setHoverInfo] = useState(null);
  const [toastMessage, setToastMessage] = useState("");
  const [toastVisible, setToastVisible] = useState(false);
  const [showRemoveDropZone, setShowRemoveDropZone] = useState(false);

  const toastTimeoutRef = useRef(null);
  const navigate = useNavigate();
  const [, setSavedRun] = useLocalStorageState("scout:lastRun", null);

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

  // Helper function to find a catalog item by type and ID, used for showing hover info in the UI.
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

  // Collect and validate the common data needed to start an execution,
  function buildExecutionContext() {
    const seed = params.global?.seed ?? Date.now();
    const existingSessionId = getExistingSessionId();
    const problemList = Array.isArray(puzzleConfig.problem) ? puzzleConfig.problem : [];

    const { isTspProblem, isVrpProblem } = validateProblemInstances({
      problemList,
      tspInstance,
      vrpInstance,
    });

    const problemParams = buildProblemParams({
      baseProblemParams: params.problem,
      tspInstance,
      vrpInstance,
      isTspProblem,
      isVrpProblem,
    });

    const searchSpaceParams = buildSearchSpaceParams({
      baseSearchSpaceParams: params.searchSpace,
      tspInstance,
      vrpInstance,
      isTspProblem,
      isVrpProblem,
    });

    return {
      seed,
      existingSessionId,
      problemParams,
      searchSpaceParams,
    };
  }
  // Save the current run configuration to localStorage and navigate to the run page with the necessary state.
  function saveAndNavigate(savedState, navigationState) {
    setSavedRun(savedState);
    navigate("/run", { state: navigationState });
  }
 // Start a runtime study by building the appropriate request payload and navigating to the run page in runtime study mode.
  async function startRuntimeStudy() {
    const { seed, existingSessionId, problemParams, searchSpaceParams } = buildExecutionContext();

    const sessionId = ensureSessionId(existingSessionId);
    const problemSizes = parseProblemSizes(params.global?.problemSizes);

    if (problemSizes.length === 0) {
      throw new Error("Please enter at least one valid problem size");
    }

    const studyId = generateId();

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

    saveAndNavigate(
      {
        pageMode: "runtimeStudy",
        loading: true,
        studyId,
        batch: null,
        studyPoints: [],
        puzzleConfig,
        params,
        tspInstance,
        vrpInstance,
        runtimeStudyRequest,
        savedAt: Date.now(),
      },
      {
        pageMode: "runtimeStudy",
        loading: true,
        puzzleConfig,
        params,
        tspInstance,
        vrpInstance,
        studyId,
        runtimeStudyRequest,
      }
    );
  }

// Start a standard run by preparing the run on the backend, building the appropriate request payload, and navigating to the run page in run mode.
  async function startStandardRun() {
    const { seed, existingSessionId, problemParams, searchSpaceParams } = buildExecutionContext();

    const prep = await prepareRun({ sessionId: existingSessionId });
    const sessionId = prep?.sessionId ?? existingSessionId;
    const runId = prep?.runId;

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

    saveAndNavigate(
      {
        pageMode: "run",
        loading: true,
        runId,
        studyId: null,
        batch: null,
        studyPoints: [],
        puzzleConfig,
        params,
        tspInstance,
        vrpInstance,
        runRequest,
        selectedRunKey: "0",
        savedAt: Date.now(),
      },
      {
        pageMode: "run",
        loading: true,
        puzzleConfig,
        params,
        tspInstance,
        vrpInstance,
        runId,
        runRequest,
      }
    );
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
  // Apply the selected template to the current lab configuration
  function onApplyTemplate(templateId) {
    if ( !templateId) return;
    // Find the matching template object from the template list
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
        templates={runTemplates}
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