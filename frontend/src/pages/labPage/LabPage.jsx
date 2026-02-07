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
      // Dropped outside - if it was a dropped piece, remove it
      if (active.id.toString().startsWith('dropped-')) {
        const fromType = active.data?.current?.fromType;
        const fromIndex = active.data?.current?.fromIndex;
        if (fromType !== undefined && fromIndex !== undefined) {
          handleRemovePiece(fromType, fromIndex);
        }
      }
      return;
    }

    // Check if dropped on the shared drop area
    if (over.id === 'shared-drop-area') {
      const pieceType = active.data?.current?.type;

      if (!pieceType) {
        return;
      }

      // Check if it's a dropped piece being moved
      if (active.id.toString().startsWith('dropped-')) {
        const fromType = active.data?.current?.fromType;
        const fromIndex = active.data?.current?.fromIndex;
        const originalId = active.data?.current?.originalId;
        const label = active.data?.current?.label;

        // If moving within same type, just reorder
        if (fromType === pieceType) {
          return; // Keep it in place
        }

        // Moving to different type - remove from old and add to new
        setPuzzleConfig(prev => {
          const newConfig = { ...prev };
          // Ensure arrays exist
          const fromArray = Array.isArray(prev[fromType]) ? prev[fromType] : [];
          const toArray = Array.isArray(prev[pieceType]) ? prev[pieceType] : [];

          // Remove from old type
          newConfig[fromType] = fromArray.filter((_, i) => i !== fromIndex);
          // Add to new type
          newConfig[pieceType] = [...toArray, {
            id: originalId,
            label: label,
            type: pieceType,
          }];
          return newConfig;
        });
      } else {
        // Adding a new piece from the selector
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
    console.log("Requesting run");

    // Use the first item from each array for now (backward compatibility)
    const searchSpace = puzzleConfig.searchSpace?.[0];
    const problem = puzzleConfig.problem?.[0];
    const algorithm = puzzleConfig.algorithm?.[0];
    const mutation = puzzleConfig.mutation?.[0];
    const acceptance = puzzleConfig.acceptance?.[0];
    const populationModel = puzzleConfig.populationModel?.[0];
    const stopCondition = puzzleConfig.stopCondition?.[0];

    // For observers, collect all IDs
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
            <RunConfigPuzzle config={puzzleConfig} onRemovePiece={handleRemovePiece} />
          </div>
          <hr className="rounded"/>
          <div className="chosen-selector-container">
            <Selector catalog={catalog} catalogLoading={catalogLoading} catalogError={catalogError} />
          </div>
        </div>
        <LabRightbar/>
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