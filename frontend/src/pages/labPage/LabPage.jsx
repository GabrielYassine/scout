import "./LabPage.css";
import LabLeftbar from "../../components/LabLeftbar/LabLeftbar.jsx";
import LabRightbar from "../../components/LabRightbar.jsx";
import RunConfigPuzzle from "../../components/runConfigPuzzle/RunConfigPuzzle.jsx";
import Selector from "../../components/selector/Selector.jsx";

import { useState } from "react";
import { DndContext, DragOverlay, rectIntersection } from "@dnd-kit/core";
import { useSessionStorageState } from "../../hooks/useSessionStorageState.js";

export default function LabPage({catalog, catalogLoading, catalogError}) {

  const [puzzleConfig, setPuzzleConfig] = useSessionStorageState(
    "scout:puzzleConfig",
    {
      searchSpace: [],
      problem: [],
      algorithm: [],
      mutation: [],
      acceptance: [],
      populationModel: [],
      stopCondition: [],
      observer: [],
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


  const [hoverInfo, setHoverInfo] = useState(null);

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
      setHoverInfo({ title: item.name, description: item.description });
   }

   function clearHover() {
      setHoverInfo(null);
   }
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
      searchSpace: [],
      problem: [],
      algorithm: [],
      mutation: [],
      acceptance: [],
      populationModel: [],
      stopCondition: [],
      observer: [],
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

  function handleRemovePiece(type, index) {
    setPuzzleConfig(prev => {
      const currentArray = Array.isArray(prev[type]) ? prev[type] : [];
      return {
        ...prev,
        [type]: currentArray.filter((_, i) => i !== index),
      };
    });
  }

  function handleDragEnd(event) {
    const { active, over } = event;
    setActiveId(null);
    setActiveLabel(null);

    if (!over) {
      if (active.id.toString().startsWith('dropped-')) {
        const fromType = active.data?.current?.fromType;
        const fromIndex = active.data?.current?.fromIndex;
        if (fromType !== undefined && fromIndex !== undefined) {
          handleRemovePiece(fromType, fromIndex);
        }
      }
      return;
    }

    if (over.id === 'shared-drop-area') {
      const pieceType = active.data?.current?.type;

      if (!pieceType) {
        return;
      }

      if (active.id.toString().startsWith('dropped-')) {
        const fromType = active.data?.current?.fromType;
        const fromIndex = active.data?.current?.fromIndex;
        const originalId = active.data?.current?.originalId;
        const label = active.data?.current?.label;

        if (fromType === pieceType) {
          return;
        }
        setPuzzleConfig(prev => {
          const newConfig = { ...prev };
          const fromArray = Array.isArray(prev[fromType]) ? prev[fromType] : [];
          const toArray = Array.isArray(prev[pieceType]) ? prev[pieceType] : [];

          newConfig[fromType] = fromArray.filter((_, i) => i !== fromIndex);
          newConfig[pieceType] = [...toArray, {
            id: originalId,
            label: label,
            type: pieceType,
          }];
          return newConfig;
        });
      } else {
        setPuzzleConfig(prev => {
          const currentArray = Array.isArray(prev[pieceType]) ? prev[pieceType] : [];
          return {
            ...prev,
            [pieceType]: [...currentArray, {
              id: active.id,
              label: active.data?.current?.label || active.id,
              type: pieceType,
            }]
          };
        });
      }
    }
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
            <RunConfigPuzzle config={puzzleConfig} onRemovePiece={handleRemovePiece}   onPieceHover={handlePieceHover}   onPieceLeave={clearHover} />
          </div>
          <hr className="rounded"/>
          <div className="chosen-selector-container">
            <Selector catalog={catalog} catalogLoading={catalogLoading} catalogError={catalogError}  onPieceHover={handlePieceHover} onPieceLeave={clearHover}/>
          </div>
        </div>
        <LabRightbar hoverInfo={hoverInfo} />
      </div>
      <DragOverlay>
        {activeId ? (
          <div className="puzzle-piece" style={{cursor: 'grabbing', boxShadow: '0 8px 24px rgba(0, 0, 0, 0.3)',}}>
            <div className="puzzle-piece-title">{activeLabel}</div>
          </div>
        ) : null}
      </DragOverlay>
    </DndContext>
  );
}