import "./LabPage.css";
import LabLeftbar from "../../components/LabLeftbar/LabLeftbar.jsx";
import LabRightbar from "../../components/LabRightbar.jsx";
import RunConfigPuzzle from "../../components/runConfigPuzzle/RunConfigPuzzle.jsx";
import Selector from "../../components/selector/Selector.jsx";
import { generatePuzzleKey } from "../../util/puzzleGenerator.js";
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
  const componentTypes = [
    "searchSpace",
    "problem",
    "algorithm",
    "mutation",
    "acceptance",
    "populationModel",
    "stopCondition",
    "observer",
  ];

  function getColIndex(type) {
    return componentTypes.indexOf(type);
  }

  function buildNeighbors(config, type, rowIndex) {
    const colIndex = getColIndex(type);
    const totalCols = componentTypes.length;

    const leftType = colIndex > 0 ? componentTypes[colIndex - 1] : null;
    const rightType = colIndex < totalCols - 1 ? componentTypes[colIndex + 1] : null;

    const leftArr = leftType ? (config[leftType] ?? []) : null;
    const rightArr = rightType ? (config[rightType] ?? []) : null;
    const curArr = config[type] ?? [];

    // Helper to extract edge from a piece's puzzleData logicalKey
    // logicalKey format: "NESW" (North, East, South, West)
    const getEdge = (piece, direction) => {
      if (!piece?.puzzleData?.logicalKey) return null;
      const key = piece.puzzleData.logicalKey;
      switch (direction) {
        case 'N': return parseInt(key[0], 10);
        case 'E': return parseInt(key[1], 10);
        case 'S': return parseInt(key[2], 10);
        case 'W': return parseInt(key[3], 10);
        default: return null;
      }
    };

    return {
      left: colIndex === 0
          ? { kind: "wall" }
          : leftArr?.[rowIndex]
            ? { kind: "piece", edge: getEdge(leftArr[rowIndex], 'E') }
            : { kind: "empty" },

      right: colIndex === totalCols - 1
          ? { kind: "wall" }
          : rightArr?.[rowIndex]
            ? { kind: "piece", edge: getEdge(rightArr[rowIndex], 'W') }
            : { kind: "empty" },

      top: rowIndex === 0
          ? { kind: "wall" }
          : curArr?.[rowIndex - 1]
            ? { kind: "piece", edge: getEdge(curArr[rowIndex - 1], 'S') }
            : { kind: "empty" },

      bottom: curArr?.[rowIndex + 1]
          ? { kind: "piece", edge: getEdge(curArr[rowIndex + 1], 'N') }
          : { kind: "empty" },
    };
  }

  function rekeyColumn(config, type, startIndex = 0) {
    const colIndex = getColIndex(type);
    const totalCols = componentTypes.length;

    const arr = Array.isArray(config[type]) ? [...config[type]] : [];
    for (let i = startIndex; i < arr.length; i++) {
      const neighbors = buildNeighbors({ ...config, [type]: arr }, type, i);

      arr[i] = {
        ...arr[i],
        puzzleData: generatePuzzleKey({
          col: colIndex,
          row: i,
          totalCols,
          neighbors,
        }),
      };
    }
    return { ...config, [type]: arr };
  }


  function handleRemovePiece(type, index) {
    setPuzzleConfig(prev => {
      const currentArray = Array.isArray(prev[type]) ? prev[type] : [];
      const nextArray = currentArray.filter((_, i) => i !== index);
      const nextConfig = { ...prev, [type]: nextArray };
      return rekeyColumn(nextConfig, type, index);
    });
  }


  function handleDragEnd(event) {
    const { active, over } = event;

    setActiveId(null);
    setActiveLabel(null);
    if (!over) {
      if (active.id.toString().startsWith("dropped-")) {
        const fromType = active.data?.current?.fromType;
        const fromIndex = active.data?.current?.fromIndex;

        if (fromType !== undefined && fromIndex !== undefined) {
          setPuzzleConfig(prev => {
            const currentArray = Array.isArray(prev[fromType]) ? prev[fromType] : [];
            const nextArray = currentArray.filter((_, i) => i !== fromIndex);

            let nextConfig = { ...prev, [fromType]: nextArray };
            return rekeyColumn(nextConfig, fromType, fromIndex);
          });
        }
      }
      return;
    }

    // Only care about dropping into puzzle area
    if (over.id !== "shared-drop-area") return;

    const pieceType = active.data?.current?.type;
    if (!pieceType) return;

    // --------------------
    // MOVE existing piece
    // --------------------
    if (active.id.toString().startsWith("dropped-")) {
      const fromType = active.data?.current?.fromType;
      const fromIndex = active.data?.current?.fromIndex;
      const originalId = active.data?.current?.originalId;
      const label = active.data?.current?.label;

      if (fromType === pieceType) return;

      setPuzzleConfig(prev => {
        const fromArray = Array.isArray(prev[fromType]) ? prev[fromType] : [];
        const toArray = Array.isArray(prev[pieceType]) ? prev[pieceType] : [];

        const newFrom = fromArray.filter((_, i) => i !== fromIndex);
        const newTo = [
          ...toArray,
          { id: originalId, label, type: pieceType },
        ];

        let nextConfig = {
          ...prev,
          [fromType]: newFrom,
          [pieceType]: newTo,
        };

        // re-key only affected pieces
        nextConfig = rekeyColumn(nextConfig, fromType, fromIndex);
        nextConfig = rekeyColumn(nextConfig, pieceType, newTo.length - 1);

        return nextConfig;
      });

      return;
    }

    // --------------------
    // ADD new piece
    // --------------------
    setPuzzleConfig(prev => {
      const currentArray = Array.isArray(prev[pieceType]) ? prev[pieceType] : [];

      const nextArray = [
        ...currentArray,
        {
          id: active.id,
          label: active.data?.current?.label || active.id,
          type: pieceType,
        },
      ];

      let nextConfig = { ...prev, [pieceType]: nextArray };
      nextConfig = rekeyColumn(nextConfig, pieceType, nextArray.length - 1);

      return nextConfig;
    });
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
            <Selector catalog={catalog} catalogLoading={catalogLoading} catalogError={catalogError}  onPieceHover={handlePieceHover} onPieceLeave={clearHover} puzzleConfig={puzzleConfig}/>
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