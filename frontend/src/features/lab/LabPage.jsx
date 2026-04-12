import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";

import LabLeftbar from "@/shared/components/sidebars/LabLeftbar.jsx";
import LabRightbar from "@/shared/components/sidebars/LabRightbar.jsx";
import RunConfigPuzzle from "@/features/lab/components/runConfigPuzzle/RunConfigPuzzle.jsx";
import Selector from "@/features/lab/components/selector/Selector.jsx";

import "./LabPage.css";

import { usePuzzleConfig } from "@/shared/contexts/PuzzleConfigContext.jsx";
import { useLocalStorageState } from "@/shared/hooks/useLocalStorageState.js";

export default function LabPage({catalog, catalogLoading, catalogError, templates, templatesLoading, templatesError}) {
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
  const toastTimeoutRef = useRef(null);
  const navigate = useNavigate();
  const [savedRun, setSavedRun, clearSavedRun] = useLocalStorageState("scout:lastRun", null);


  useEffect(() => {
    return () => {
      if (toastTimeoutRef.current) {
        clearTimeout(toastTimeoutRef.current);
      }
    };
  }, []);

  function showToast(message) {
    if (toastVisible) return;
    setToastMessage(message);
    setToastVisible(true);
    if (toastTimeoutRef.current) {
      clearTimeout(toastTimeoutRef.current);
    }
    toastTimeoutRef.current = setTimeout(() => {
      setToastVisible(false);
    }, 3500);
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

  const parseProblemSizes = (text) =>
    String(text ?? "")
      .split(",")
      .map((s) => Number(s.trim()))
      .filter((n) => Number.isInteger(n) && n > 0);

 async function onRun() {
   try {
     const experimentType = params.global?.experimentType ?? "run";
     const seed = params.global?.seed ?? Date.now();

     const problemList = Array.isArray(puzzleConfig.problem) ? puzzleConfig.problem : [];
     const problemParams = { ...params.problem };

     if (tspInstance && tspInstance.cities && tspInstance.cities.length > 0) {
       problemParams.tspInstance = tspInstance;
     }

     const isVrpProblem = problemList.some((p) => p.id === "vrp");
     if (isVrpProblem && vrpInstance) {
       problemParams.vrpInstance = vrpInstance;
     }

     const searchSpaceParams = { ...params.searchSpace };
     const isTSPProblem = problemList.some((p) => p.id === "tsp");

     if (isTSPProblem && tspInstance && tspInstance.cities && tspInstance.cities.length > 0) {
       searchSpaceParams.n = tspInstance.cities.length;
     }

     if (isVrpProblem && vrpInstance) {
       searchSpaceParams.vrpInstance = vrpInstance;
     }

     if (experimentType === "runtimeStudy") {
       const repetitionsPerSize = params.global?.repetitionsPerSize ?? 30;
       const problemSizes = parseProblemSizes(params.global?.problemSizes);
       const wsUpdateEverySizes = params.global?.wsUpdateEverySizes ?? 1;
       const studyId = window.crypto?.randomUUID
         ? window.crypto.randomUUID()
         : `${Date.now()}-${Math.random().toString(16).slice(2)}`;

       if (problemSizes.length === 0) {
         throw new Error("Please enter at least one valid problem size");
       }

       const body = {
         studyId,
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
         repetitionsPerSize,
         wsUpdateEverySizes,
       };
        setSavedRun({
          pageMode: "runtimeStudy",
          loading: true,
          studyId,
          batch: null,
          studyPoints: [],
          puzzleConfig,
          params,
          tspInstance,
          vrpInstance,
          runtimeStudyRequest: body,
          savedAt: Date.now(),
        });


       navigate("/run", {
         state: {
           pageMode: "runtimeStudy",
           loading: true,
           puzzleConfig,
           params,
           tspInstance,
           vrpInstance,
           studyId,
           runtimeStudyRequest: body,
         },
       });
       return;
     }

     const runTimes = params.global?.runTimes ?? 1;
     const logEveryIterations = params.global?.logEveryIterations ?? 100;
     const wsUpdateEveryIterations = params.global?.wsUpdateEveryIterations ?? 100;
     const runId = window.crypto?.randomUUID
       ? window.crypto.randomUUID()
       : `${Date.now()}-${Math.random().toString(16).slice(2)}`;

     const body = {
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
       runTimes,
       runId,
       logEveryIterations,
       wsUpdateEveryIterations,
     };

     const res = await fetch("/api/run", {
       method: "POST",
       headers: { "Content-Type": "application/json" },
       body: JSON.stringify(body),
     });

     if (!res.ok) {
       let message = `Run failed with status ${res.status}`;
       try {
         const data = await res.json();
         if (data?.message) {
           message = data.message;
         }
       } catch {
         console.error("Failed to parse error response as JSON");
       }
       throw new Error(message);
     }

     setSavedRun({
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
        selectedRunKey: "0",
        savedAt: Date.now(),
    });

     navigate("/run", {
       state: { pageMode: "run", loading: true, puzzleConfig, params, tspInstance, vrpInstance, runId },
     });
   } catch (err) {
     showToast(err.message || "Failed to start run");
   }
 }
  function onApplyTemplate(templateId) {
    if (!catalog || !templateId) return;
    const tpl = (templates ?? []).find((t) => t.id === templateId);
    if (!tpl) return;
    applyTemplateRunRequest(tpl.runRequest, catalog);
  }

  return (
    <div className="lab-page">
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
          templates={templates}
          templatesLoading={templatesLoading}
          templatesError={templatesError}
          onApplyTemplate={onApplyTemplate}
      />
      <div className="lab-page-content">
        <div className="selector-timeline">
          <RunConfigPuzzle
            onPieceHover={handlePieceHover}
            onPieceLeave={clearHover}
          />
        </div>
        <hr className="rounded"/>
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
