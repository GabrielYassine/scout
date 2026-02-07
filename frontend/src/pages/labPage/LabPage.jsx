import "./LabPage.css";
import LabLeftbar from "../../components/LabLeftbar/LabLeftbar.jsx";
import LabRightbar from "../../components/LabRightbar.jsx";
import RunConfigPuzzle from "../../components/runConfigPuzzle/RunConfigPuzzle.jsx";
import RepresentationSelector from "../../components/selector/representationSelector/RepresentationSelector.jsx";
import ProblemSelector from "../../components/selector/problemSelector/ProblemSelector.jsx";
import AlgorithmSelector from "../../components/selector/algorithmSelector/AlgorithmSelector.jsx";
import MutationSelector from "../../components/selector/mutationSelector/MutationSelector.jsx";
import AcceptanceSelector from "../../components/selector/acceptanceSelector/AcceptanceSelector.jsx";
import PopulationModelSelector from "../../components/selector/PopulationSelector/PopulationSelector.jsx";
import StopConditionSelector from "../../components/selector/stopConditionSelector/StopConditionSelector.jsx";
import ObserverSelector from "../../components/selector/observerSelector/ObserverSelector.jsx";

import { useState } from "react";
import { DndContext, DragOverlay, rectIntersection } from "@dnd-kit/core";
import { useSessionStorageState } from "../../hooks/useSessionStorageState.js";

const SELECTORS = [
  { id: "searchSpace", Component: RepresentationSelector },
  { id: "problem", Component: ProblemSelector },
  { id: "algorithm", Component: AlgorithmSelector },
  { id: "mutation", Component: MutationSelector },
  { id: "acceptance", Component: AcceptanceSelector },
  { id: "populationModel", Component: PopulationModelSelector },
  { id: "stopCondition", Component: StopConditionSelector },
  { id: "observer", Component: ObserverSelector },
];

export default function LabPage({catalog, catalogLoading, catalogError}) {
  const [currentSelectorIndex, setCurrentSelectorIndex] = useSessionStorageState(
    "scout:currentSelector",
    0
  );

  const [puzzleConfig, setPuzzleConfig] = useSessionStorageState(
    "scout:puzzleConfig",
    {
      searchSpace: null,
      problem: null,
      algorithm: null,
      mutation: null,
      acceptance: null,
      populationModel: null,
      stopCondition: null,
      observer: null,
    }
  );

  const [params, setParams] = useSessionStorageState(
    "scout:puzzleParams",
    {
      searchSpace: {},
      problem: {},
      algorithm: {},
      mutation: {},
      acceptance: {},
      populationModel: {},
      stopCondition: {},
      observer: {},
    }
  );

  const [activeId, setActiveId] = useState(null);
  const [activeLabel, setActiveLabel] = useState(null);

  const CurrentSelector = SELECTORS[currentSelectorIndex].Component;

  function handleDragStart(event) {
    const { active } = event;
    setActiveId(active.id);
    setActiveLabel(active.data?.current?.label || active.id);
  }

  function handleParamChange(type, newParams) {
    setParams(prev => ({
      ...prev,
      [type]: newParams,
    }));
  }

  function handleReset() {
    setPuzzleConfig({
      searchSpace: null,
      problem: null,
      algorithm: null,
      mutation: null,
      acceptance: null,
      populationModel: null,
      stopCondition: null,
      observer: null,
    });
    setParams({
      searchSpace: {},
      problem: {},
      algorithm: {},
      mutation: {},
      acceptance: {},
      populationModel: {},
      stopCondition: {},
      observer: {},
    });
  }

  function handleDragEnd(event) {
    const { active, over } = event;
    setActiveId(null);
    setActiveLabel(null);

    if (!over) {
      if (active.id.toString().startsWith('dropped-')) {
        const fromZone = active.data?.current?.fromZone;
        setPuzzleConfig(prev => ({
          ...prev,
          [fromZone]: null,
        }));
      }
      return;
    }

    const pieceType = active.data?.current?.type;
    const acceptsType = over.data?.current?.acceptsType;
    if (pieceType !== acceptsType) {
      return;
    }
    if (active.id.toString().startsWith('dropped-')) {
      const fromZone = active.data?.current?.fromZone;
      const originalId = active.data?.current?.originalId;
      if (over.id !== fromZone) {
        setPuzzleConfig(prev => ({
          ...prev,
          [fromZone]: null,
          [over.id]: {
            id: originalId,
            label: active.data?.current?.label || originalId,
            type: pieceType,
          }
        }));
      }
      return;
    }
    setPuzzleConfig(prev => ({
      ...prev,
      [over.id]: {
        id: active.id,
        label: active.data?.current?.label || active.id,
        type: pieceType,
      }
    }));
  }

  function handlePrevious() {
    setCurrentSelectorIndex((prev) => Math.max(0, prev - 1));
  }

  function handleNext() {
    setCurrentSelectorIndex((prev) => Math.min(SELECTORS.length - 1, prev + 1));
  }

  async function onRun() {
    console.log("Requesting run");
    const res = await fetch("/api/run", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        searchSpaceId: puzzleConfig.searchSpace?.id,
        searchSpaceParams: params.searchSpace,
        problemId: puzzleConfig.problem?.id,
        problemParams: params.problem,
        algorithmId: puzzleConfig.algorithm?.id,
        algorithmParams: params.algorithm,
        mutationId: puzzleConfig.mutation?.id,
        mutationParams: params.mutation,
        acceptanceRuleId: puzzleConfig.acceptance?.id,
        acceptanceRuleParams: params.acceptance,
        populationModelId: puzzleConfig.populationModel?.id,
        populationModelParams: params.populationModel,
        observerIds: puzzleConfig.observer?.id ? [puzzleConfig.observer.id] : [],
        stopConditionId: puzzleConfig.stopCondition?.id,
        stopConditionParams: params.stopCondition,
        seed: Date.now(), // We need to provide a field for seed later!
      }),
    });

    const result = await res.json();
    console.log(result);
  }

  return (
    <DndContext
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
      collisionDetection={rectIntersection}
    >
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
            <RunConfigPuzzle config={puzzleConfig} />
          </div>
          <hr className="rounded"/>
          <CurrentSelector catalog={catalog} catalogLoading={catalogLoading} catalogError={catalogError} />
          <div className="navigation-buttons">
            <button className="btn btn--red" type="button"
                onClick={handlePrevious}
                disabled={currentSelectorIndex === 0}
            >
              Previous
            </button>
            <button className="btn btn--green" type="button"
                onClick={handleNext}
                disabled={currentSelectorIndex === SELECTORS.length - 1}
            >
              Next
            </button>
          </div>
        </div>
        <LabRightbar/>
      </div>
      <DragOverlay>
        {activeId ? (
          <div className="puzzle-piece" style={{
            cursor: 'grabbing',
            boxShadow: '0 8px 24px rgba(0, 0, 0, 0.3)',
          }}>
            <div className="puzzle-piece-title">{activeLabel}</div>
          </div>
        ) : null}
      </DragOverlay>
    </DndContext>
  );
}