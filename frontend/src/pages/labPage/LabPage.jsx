import "./LabPage.css";
import LabLeftbar from "../../components/LabLeftbar/LabLeftbar.jsx";
import LabRightbar from "../../components/LabRightbar.jsx";
import RunConfigPuzzle from "../../components/runConfigPuzzle/RunConfigPuzzle.jsx";
import Selector from "../../components/selector/Selector.jsx";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { usePuzzleConfig } from "../../contexts/PuzzleConfigContext.jsx";

export default function LabPage({catalog, catalogLoading, catalogError}) {
  const {
    puzzleConfig,
    params,
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
        algorithm: catalog.algorithms,
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
    const searchSpace = puzzleConfig.searchSpace?.[0];
    const problem = puzzleConfig.problem?.[0];
    const algorithm = puzzleConfig.algorithm?.[0];
    const mutation = puzzleConfig.mutation?.[0];
    const acceptance = puzzleConfig.acceptance?.[0];
    const populationModel = puzzleConfig.populationModel?.[0];
    const stopCondition = puzzleConfig.stopCondition?.[0];

    const observerIds = puzzleConfig.observer?.map(obs => obs.id) || [];

    const res = await fetch("/api/run", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        searchSpaceId: searchSpace?.id,
        searchSpaceParams: params.searchSpace,
        problemId: problem?.id,
        problemParams: params.problem,
        algorithmId: algorithm?.id,
        algorithmParams: params.algorithm,
        mutationId: mutation?.id,
        mutationParams: params.mutation,
        acceptanceRuleId: acceptance?.id,
        acceptanceRuleParams: params.acceptance,
        populationModelId: populationModel?.id,
        populationModelParams: params.populationModel,
        observerIds: observerIds,
        stopConditionId: stopCondition?.id,
        stopConditionParams: params.stopCondition,
        seed: Date.now(),
      }),
    });

    console.log("RUN REQUEST BODY:", {
      searchSpaceId: searchSpace?.id,
      searchSpaceParams: params.searchSpace,
      problemId: problem?.id,
      problemParams: params.problem,
      algorithmId: algorithm?.id,
      algorithmParams: params.algorithm,
      mutationId: mutation?.id,
      mutationParams: params.mutation,
      acceptanceRuleId: acceptance?.id,
      acceptanceRuleParams: params.acceptance,
      populationModelId: populationModel?.id,
      populationModelParams: params.populationModel,
      stopConditionId: stopCondition?.id,
      stopConditionParams: params.stopCondition,
      observerIds,
    });

    const result = await res.json();
    console.log(result);
    navigate("/run", {
      state: {
        run: result,
        puzzleConfig,
        params,
      },
    });
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
      <LabRightbar hoverInfo={hoverInfo} />
    </div>
  );
}
