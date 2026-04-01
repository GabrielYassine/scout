import "./LabPage.css";

import LabLeftbar from "../components/SideBars/LabLeftbar.jsx";
import LabRightbar from "../components/SideBars/LabRightbar.jsx";
import RunConfigPuzzle from "../components/runConfigPuzzle/RunConfigPuzzle.jsx";
import Selector from "../components/selector/Selector.jsx";
import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { usePuzzleConfig } from "../contexts/PuzzleConfigContext.jsx";

export default function LabPage({catalog, catalogLoading, catalogError, templates, templatesLoading, templatesError}) {
  const {
    puzzleConfig,
    params,
    tspInstance,
    setTspInstance,
    applyTemplateRunRequest,
    handleParamChange,
    handleReset,
  } = usePuzzleConfig();

  const [hoverInfo, setHoverInfo] = useState(null);
  const [toastMessage, setToastMessage] = useState("");
  const [toastVisible, setToastVisible] = useState(false);
  const toastTimeoutRef = useRef(null);
  const navigate = useNavigate();

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

  async function onRun() {
    const seed = params.global?.seed ?? Date.now();
    const runTimes = params.global?.runTimes ?? 1;
    const wsUpdateEveryIterations = params.global?.wsUpdateEveryIterations ?? 100;
    const runId = window.crypto?.randomUUID ? window.crypto.randomUUID() : `${Date.now()}-${Math.random().toString(16).slice(2)}`;

    const problemParams = { ...params.problem };
    if (tspInstance && tspInstance.cities && tspInstance.cities.length > 0) {
      problemParams.tspInstance = tspInstance;
    }

    const searchSpaceParams = { ...params.searchSpace };
    const isTSPProblem = puzzleConfig.problem?.some((p) => p.id === "tsp");
    if (isTSPProblem && tspInstance && tspInstance.cities && tspInstance.cities.length > 0) {
      searchSpaceParams.n = tspInstance.cities.length;
    }

    const body = {
      searchSpaceId: puzzleConfig.searchSpace?.[0]?.id ?? null,
      searchSpaceParams: searchSpaceParams,

      problemIds: puzzleConfig.problem?.map((x) => x.id) ?? [],
      problemParams: problemParams,


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
      seed: seed,
      runTimes: runTimes,
      runId,
      wsUpdateEveryIterations,
    };

    try {
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
        } catch (e) {
          // ignore parse errors
        }
        throw new Error(message);
      }

      navigate("/run", {
        state: { loading: true, puzzleConfig, params, tspInstance, runId },
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
            catalogLoading={catalogLoading}
            catalogError={catalogError}
            onPieceHover={handlePieceHover}
            onPieceLeave={clearHover}
            puzzleConfig={puzzleConfig}
          />
        </div>
      </div>
      <LabRightbar
        hoverInfo={hoverInfo}
        tspInstance={tspInstance}
        onTspInstanceChange={setTspInstance}
      />
    </div>
  );
}
