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

function generateId() {
  return window.crypto?.randomUUID
    ? window.crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

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

function validateProblemInstances({ problemList, tspInstance, vrpInstance }) {
  const { isTspProblem, isVrpProblem } = getProblemFlags(problemList);

  const hasValidTspInstance =
    Array.isArray(tspInstance?.cities) && tspInstance.cities.length > 0;

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

function buildProblemParams({ baseProblemParams, tspInstance, vrpInstance, isVrpProblem }) {
  const problemParams = { ...baseProblemParams };

  if (tspInstance?.cities?.length > 0) {
    problemParams.tspInstance = tspInstance;
  }

  if (isVrpProblem && vrpInstance) {
    problemParams.vrpInstance = vrpInstance;
  }

  return problemParams;
}

function buildSearchSpaceParams({
  baseSearchSpaceParams,
  tspInstance,
  vrpInstance,
  isTspProblem,
  isVrpProblem,
}) {
  const searchSpaceParams = { ...baseSearchSpaceParams };

  if (isTspProblem && tspInstance?.cities?.length > 0) {
    searchSpaceParams.n = tspInstance.cities.length;
  }

  if (isVrpProblem && vrpInstance) {
    searchSpaceParams.vrpInstance = vrpInstance;
  }

  return searchSpaceParams;
}

function getExistingSessionId() {
  return window.sessionStorage?.getItem(SESSION_STORAGE_KEY) ?? null;
}

function persistSessionId(sessionId) {
  try {
    window.sessionStorage?.setItem(SESSION_STORAGE_KEY, sessionId);
  } catch {
    // ignore storage errors
  }
}

function ensureSessionId(existingSessionId) {
  if (existingSessionId) return existingSessionId;
  const next = generateId();
  persistSessionId(next);
  return next;
}

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


// ---------- Main component ----------
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

// Listen to drag-and-drop events from dnd-kit to determine when to show the remove drop zone overlay
  useDndMonitor({
    onDragStart(event) {
      const isPlacedPiece = event.active?.data?.current?.fromIndex != null;
      setShowRemoveDropZone(isPlacedPiece);
    },
    onDragOver(event) {
      const isPlacedPiece = event.active?.data?.current?.fromIndex != null;
      if (!isPlacedPiece) {
        setShowRemoveDropZone(false);
        return;
      }

      setShowRemoveDropZone(event.over?.id !== SHARED_DROP_AREA_ID);
    },
    onDragEnd() {
      setShowRemoveDropZone(false);
    },
    onDragCancel() {
      setShowRemoveDropZone(false);
    },
  });

  useEffect(() => {
    return () => {
      if (toastTimeoutRef.current) {
        clearTimeout(toastTimeoutRef.current);
      }
    };
  }, []);

  function showToast(message) {
    setToastMessage(message);
    setToastVisible(true);

    if (toastTimeoutRef.current) {
      clearTimeout(toastTimeoutRef.current);
    }

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

  function handlePieceHover(type, id) {
    const item = getCatalogItem(type, id);
    if (!item) return;
    setHoverInfo({ title: item.displayName, description: item.description });
  }

  function clearHover() {
    setHoverInfo(null);
  }

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

  function saveAndNavigate(savedState, navigationState) {
       setSavedRun(savedState);
       navigate("/run", { state: navigationState });
  }

  async function startRuntimeStudy() {
    const { seed, existingSessionId, problemParams, searchSpaceParams } =
      buildExecutionContext();

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

  async function startStandardRun() {
    const { seed, existingSessionId, problemParams, searchSpaceParams } =
      buildExecutionContext();

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

  function onApplyTemplate(templateId) {
    if (!catalog || !templateId) return;
    const tpl = runTemplates.find((t) => t.id === templateId);
    if (!tpl) return;
    applyTemplateRunRequest(tpl.runRequest, catalog);
  }

  return (
    <div className="lab-page">
         {/* Overlay shown when dragging a piece from the selector to indicate the remove area */}
      {showRemoveDropZone && <div className="lab-remove-overlay" />}
         {/* Toast message */}
      {toastVisible && (
        <div className="lab-toast" role="status" aria-live="polite">
          {toastMessage}
        </div>
      )}
        {/* Left sidebar: parameters, templates, run/reset buttons */}
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
        {/* Main middle content */}
      <div className="lab-page-content">
        <div className="selector-timeline">
          <RunConfigPuzzle
            onPieceHover={handlePieceHover}
            onPieceLeave={clearHover}
          />
        </div>

        <hr className="rounded" />

        <div className="chosen-selector-container">
          {/* Area showing selectable available pieces */}
          <Selector
            catalog={catalog}
            onPieceHover={handlePieceHover}
            onPieceLeave={clearHover}
            puzzleConfig={puzzleConfig}
            params={params}
          />
        </div>
      </div>
      {/* Right sidebar: hover description + problem instance editor */}
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