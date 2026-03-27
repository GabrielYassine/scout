import "./LabPage.css";

import LabLeftbar from "../../components/LabLeftbar/LabLeftbar.jsx";
import LabRightbar from "../../components/LabRightbar.jsx";
import RunConfigPuzzle from "../../components/runConfigPuzzle/RunConfigPuzzle.jsx";
import Selector from "../../components/selector/Selector.jsx";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { usePuzzleConfig } from "../../contexts/PuzzleConfigContext.jsx";

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
  const navigate = useNavigate();

  function getCatalogItem(type, id) {
      if (!catalog || !id) return null;

      const map = {
        searchSpace: catalog.searchSpaces,
        problem: catalog.problems,
        generator: catalog.generators,
        selection: catalog.selectionRules,
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
      searchSpaceId: puzzleConfig.searchSpace?.map((x) => x.id) ?? [],
      searchSpaceParams: searchSpaceParams,

      problemId: puzzleConfig.problem?.map((x) => x.id) ?? [],
      problemParams: problemParams,


      generatorId: puzzleConfig.generator?.map((x) => x.id) ?? [],
      generatorParams: params.generator,

      selectionRuleId: puzzleConfig.selection?.map((x) => x.id) ?? [],
      selectionRuleParams: params.selection,

      populationModelId: puzzleConfig.populationModel?.map((x) => x.id) ?? [],
      populationModelParams: params.populationModel,

      stopConditionId: puzzleConfig.stopCondition?.map((x) => x.id) ?? [],
      stopConditionParams: params.stopCondition,

      observerIds: puzzleConfig.observer?.map((x) => x.id) ?? [],
      observerParams: params.observer,
      seed: seed,
      runTimes: runTimes,
      runId,
      wsUpdateEveryIterations,
    };

    // Navigate immediately to show loading state while the run is being prepared
    navigate("/run", {
      state: { loading: true, puzzleConfig, params, tspInstance, runId },
    });

    try {
      const res = await fetch("/api/run", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });

      if (!res.ok) {
        throw new Error(`Run failed with status ${res.status}`);
      }
    } catch (err) {
      navigate("/run", {
        state: { error: err.message || "Failed to start run", puzzleConfig, params, tspInstance, runId },
        replace: true,
      });
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
