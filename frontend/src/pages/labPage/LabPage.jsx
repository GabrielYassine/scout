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
    applyTemplateRunRequest,
    handleParamChange,
    handleReset,
  } = usePuzzleConfig();

  const [hoverInfo, setHoverInfo] = useState(null);
  const [tspInstance, setTspInstance] = useState({
    name: "Default Instance",
    cities: [
      { id: 0, x: 50, y: 0 },
      { id: 1, x: 100, y: 0 },
      { id: 2, x: 100, y: 100 },
      { id: 3, x: 50, y: 100 },
    ]
  });
  const navigate = useNavigate();

  function getCatalogItem(type, id) {
      if (!catalog || !id) return null;

      const map = {
        searchSpace: catalog.searchSpaces,
        problem: catalog.problems,
        mutation: catalog.mutations,
        acceptance: catalog.acceptanceRules,
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

    // Prepare problem params with TSP instance if available
    const problemParams = { ...params.problem };
    if (tspInstance && tspInstance.cities && tspInstance.cities.length > 0) {
      problemParams.tspInstance = tspInstance;
    }

    // Prepare search space params - set n from TSP instance if TSP problem is selected
    const searchSpaceParams = { ...params.searchSpace };
    const isTSPProblem = puzzleConfig.problem?.some(p => p.id === 'tsp');
    if (isTSPProblem && tspInstance && tspInstance.cities && tspInstance.cities.length > 0) {
      searchSpaceParams.n = tspInstance.cities.length;
    }

    const body = {
      searchSpaceId: puzzleConfig.searchSpace?.map((x) => x.id) ?? [],
      searchSpaceParams: searchSpaceParams,

      problemId: puzzleConfig.problem?.map((x) => x.id) ?? [],
      problemParams: problemParams,


      mutationId: puzzleConfig.mutation?.map((x) => x.id) ?? [],
      mutationParams: params.mutation,

      acceptanceRuleId: puzzleConfig.acceptance?.map((x) => x.id) ?? [],
      acceptanceRuleParams: params.acceptance,

      populationModelId: puzzleConfig.populationModel?.map((x) => x.id) ?? [],
      populationModelParams: params.populationModel,

      stopConditionId: puzzleConfig.stopCondition?.map((x) => x.id) ?? [],
      stopConditionParams: params.stopCondition,

      observerIds: puzzleConfig.observer?.map((x) => x.id) ?? [],
      seed: seed,
      runTimes: runTimes,
    };

    const res = await fetch("/api/run", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });

    console.log("RUN REQUEST BODY:", body);

    const batch = await res.json();
    console.log(batch);
    navigate("/run", {
      state: {
        batch,
        puzzleConfig,
        params,
      },
    });
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
