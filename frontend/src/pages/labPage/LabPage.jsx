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

    const body = {
      searchSpaceId: puzzleConfig.searchSpace?.map((x) => x.id) ?? [],
      searchSpaceParams: params.searchSpace,

      problemId: puzzleConfig.problem?.map((x) => x.id) ?? [],
      problemParams: params.problem,

      algorithmId: puzzleConfig.algorithm?.map((x) => x.id) ?? [],
      algorithmParams: params.algorithm,

      mutationId: puzzleConfig.mutation?.map((x) => x.id) ?? [],
      mutationParams: params.mutation,

      acceptanceRuleId: puzzleConfig.acceptance?.map((x) => x.id) ?? [],
      acceptanceRuleParams: params.acceptance,

      populationModelId: puzzleConfig.populationModel?.map((x) => x.id) ?? [],
      populationModelParams: params.populationModel,

      stopConditionId: puzzleConfig.stopCondition?.map((x) => x.id) ?? [],
      stopConditionParams: params.stopCondition,

      observerIds: puzzleConfig.observer?.map((x) => x.id) ?? [],
      seed: Date.now(),
    };

    const res = await fetch("/api/run", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });

    console.log("RUN REQUEST BODY:", body);

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
