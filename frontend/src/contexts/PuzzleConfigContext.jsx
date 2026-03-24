import { createContext, useContext, useMemo, useState } from "react";
import { DndContext, DragOverlay, rectIntersection } from "@dnd-kit/core";
import { useSessionStorageState } from "../hooks/useSessionStorageState.js";
import { generatePuzzleKey } from "../util/puzzleGenerator.js";
import { maskStyle } from "../util/puzzleMasks.js";
import "../components/puzzlePiece/PuzzlePiece.css";

const PuzzleConfigContext = createContext(null);

export const usePuzzleConfig = () => {
  const context = useContext(PuzzleConfigContext);
  if (!context) throw new Error("usePuzzleConfig must be used within PuzzleConfigProvider");
  return context;
};

const componentTypes = [
  "searchSpace",
  "problem",
  "generator",
  "acceptance",
  "populationModel",
  "stopCondition",
  "observer",
];

export const DEFAULT_TSP_INSTANCE = {
  name: "Default Instance",
  cities: [
    { id: 0, x: 50, y: 0 },
    { id: 1, x: 100, y: 0 },
    { id: 2, x: 100, y: 100 },
    { id: 3, x: 50, y: 100 },
  ],
};

const cloneTspInstance = (tsp = DEFAULT_TSP_INSTANCE) => ({
  ...tsp,
  cities: (tsp.cities ?? []).map((c) => ({ ...c })),
});

const createEmptyPuzzleConfig = () => ({
  searchSpace: [],
  problem: [],
  generator: [],
  acceptance: [],
  populationModel: [],
  stopCondition: [],
  observer: [],
});

const createEmptyParams = () => ({
  global: {},
  searchSpace: {},
  problem: {},
  generator: {},
  acceptance: {},
  populationModel: {},
  stopCondition: {},
  observer: {},
});

const createDefaultConfig = (id, name) => ({
  id,
  name,
  puzzleConfig: createEmptyPuzzleConfig(),
  params: createEmptyParams(),
  tspInstance: cloneTspInstance(DEFAULT_TSP_INSTANCE),
});

export function PuzzleConfigProvider({ children }) {
  const [configs, setConfigs] = useSessionStorageState("scout:runConfigs", [
    createDefaultConfig("config-1", "Config 1"),
  ]);

  const [activeConfigId, setActiveConfigId] = useSessionStorageState(
    "scout:activeConfigId",
    "config-1"
  );

  const [activeDrag, setActiveDrag] = useState(null);

  const activeConfig = useMemo(
    () => configs.find((c) => c.id === activeConfigId) || configs[0],
    [configs, activeConfigId]
  );

  const puzzleConfig = activeConfig?.puzzleConfig ?? createEmptyPuzzleConfig();
  const params = activeConfig?.params ?? createEmptyParams();
  const tspInstance = activeConfig?.tspInstance
    ? cloneTspInstance(activeConfig.tspInstance)
    : cloneTspInstance(DEFAULT_TSP_INSTANCE);

  const updateActiveConfig = (key, updater) => {
    setConfigs((prev) =>
      prev.map((config) =>
        config.id === activeConfigId
          ? { ...config, [key]: typeof updater === "function" ? updater(config[key]) : updater }
          : config
      )
    );
  };

  const setPuzzleConfig = (updater) => updateActiveConfig("puzzleConfig", updater);
  const setParams = (updater) => updateActiveConfig("params", updater);
  const setTspInstance = (updater) => updateActiveConfig("tspInstance", updater);

  const addNewConfig = () => {
    const newId = `config-${Date.now()}`;
    const newConfig = createDefaultConfig(newId, `Config ${configs.length + 1}`);
    setConfigs((prev) => [...prev, newConfig]);
    setActiveConfigId(newId);
  };

  const deleteConfig = (configId) => {
    setConfigs((prev) => {
      if (prev.length === 1) return prev;

      const next = prev.filter((c) => c.id !== configId);

      if (activeConfigId === configId) {
        setActiveConfigId(next[0]?.id ?? "config-1");
      }

      return next;
    });
  };

  const renameConfig = (configId, newName) => {
    setConfigs((prev) => prev.map((c) => (c.id === configId ? { ...c, name: newName } : c)));
  };

  function getColIndex(type) {
    return componentTypes.indexOf(type);
  }

  function buildNeighbors(config, type, rowIndex) {
    const colIndex = getColIndex(type);
    const totalCols = componentTypes.length;

    const leftType = colIndex > 0 ? componentTypes[colIndex - 1] : null;
    const rightType = colIndex < totalCols - 1 ? componentTypes[colIndex + 1] : null;

    const leftArr = leftType ? config[leftType] ?? [] : null;
    const rightArr = rightType ? config[rightType] ?? [] : null;
    const curArr = config[type] ?? [];

    const getEdge = (piece, direction) => {
      if (!piece?.puzzleData?.logicalKey) return null;
      const key = piece.puzzleData.logicalKey;
      switch (direction) {
        case "N":
          return parseInt(key[0], 10);
        case "E":
          return parseInt(key[1], 10);
        case "S":
          return parseInt(key[2], 10);
        case "W":
          return parseInt(key[3], 10);
        default:
          return null;
      }
    };

    return {
      left:
        colIndex === 0
          ? { kind: "wall" }
          : leftArr?.[rowIndex]
          ? { kind: "piece", edge: getEdge(leftArr[rowIndex], "E") }
          : { kind: "empty" },

      right:
        colIndex === totalCols - 1
          ? { kind: "wall" }
          : rightArr?.[rowIndex]
          ? { kind: "piece", edge: getEdge(rightArr[rowIndex], "W") }
          : { kind: "empty" },

      top:
        rowIndex === 0
          ? { kind: "wall" }
          : curArr?.[rowIndex - 1]
          ? { kind: "piece", edge: getEdge(curArr[rowIndex - 1], "S") }
          : { kind: "empty" },

      bottom: curArr?.[rowIndex + 1]
        ? { kind: "piece", edge: getEdge(curArr[rowIndex + 1], "N") }
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

  function handleDragStart({ active }) {
    setActiveDrag({
      id: active.id,
      label: active.data?.current?.label || active.id,
      type: active.data?.current?.type || null,
    });
  }

  function handleRemovePiece(type, index) {
    setPuzzleConfig((prev) => {
      const currentArray = Array.isArray(prev[type]) ? prev[type] : [];
      const nextArray = currentArray.filter((_, i) => i !== index);
      const nextConfig = { ...prev, [type]: nextArray };
      return rekeyColumn(nextConfig, type, index);
    });
  }

  function handleDragEnd({ active, over }) {
    setActiveDrag(null);
    if (!over) {
      if (active.id.toString().startsWith("dropped-")) {
        const { fromType, fromIndex } = active.data?.current ?? {};
        if (fromType != null && fromIndex != null) {
          handleRemovePiece(fromType, fromIndex);
        }
      }
      return;
    }

    if (over.id !== "shared-drop-area") return;

    const pieceType = active.data?.current?.type;
    if (!pieceType) return;

    if (active.id.toString().startsWith("dropped-")) return;

    setPuzzleConfig((prev) => {
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

  function handleParamChange(type, newParams) {
    setParams((prev) => ({ ...prev, [type]: newParams }));
  }

  function handleReset() {
    setPuzzleConfig(createEmptyPuzzleConfig());
    setParams(createEmptyParams());
  }

  function applyTemplateRunRequest(runRequest, catalog) {
    if (!runRequest || !catalog) return;

    const mapIdsToPieces = (ids, catalogItems, colType) =>
      (ids || [])
        .map((id) => {
          const item = catalogItems.find((x) => x.id === id);
          return item ? { id: item.id, label: item.displayName, type: colType } : null;
        })
        .filter(Boolean);

    const componentMapping = [
      { type: "searchSpace", catalogKey: "searchSpaces", requestKey: "searchSpaceId" },
      { type: "problem", catalogKey: "problems", requestKey: "problemId" },
      { type: "generator", catalogKey: "generators", requestKey: "generatorId" },
      { type: "acceptance", catalogKey: "acceptanceRules", requestKey: "acceptanceRuleId" },
      { type: "populationModel", catalogKey: "populationModels", requestKey: "populationModelId" },
      { type: "stopCondition", catalogKey: "stopConditions", requestKey: "stopConditionId" },
      { type: "observer", catalogKey: "observers", requestKey: "observerIds" },
    ];

    const newPuzzleConfig = createEmptyPuzzleConfig();
    componentMapping.forEach(({ type, catalogKey, requestKey }) => {
      if (catalog[catalogKey]) {
        newPuzzleConfig[type] = mapIdsToPieces(runRequest[requestKey], catalog[catalogKey], type);
      }
    });

    const finalConfig = componentTypes.reduce((cfg, type) => {
      return cfg[type]?.length > 0 ? rekeyColumn(cfg, type, 0) : cfg;
    }, newPuzzleConfig);

    setPuzzleConfig(finalConfig);

    setParams({
      global: {
        seed: runRequest.seed || Date.now(),
        runTimes: runRequest.runTimes || 1,
        wsUpdateEveryIterations: runRequest.wsUpdateEveryIterations || 100,
      },
      searchSpace: runRequest.searchSpaceParams || {},
      problem: runRequest.problemParams || {},
      generator: runRequest.generatorParams || {},
      acceptance: runRequest.acceptanceRuleParams || {},
      populationModel: runRequest.populationModelParams || {},
      stopCondition: runRequest.stopConditionParams || {},
      observer: {},
    });
  }

  const value = {
    configs,
    activeConfigId,
    activeConfig,
    puzzleConfig,
    params,
    tspInstance,

    setActiveConfigId,
    addNewConfig,
    deleteConfig,
    renameConfig,

    handleRemovePiece,
    handleParamChange,
    handleReset,
    applyTemplateRunRequest,

    setTspInstance,

    activeId: activeDrag?.id ?? null,
    activeLabel: activeDrag?.label ?? null,
  };

  const overlayBg = activeDrag?.type
    ? `var(--color-${activeDrag.type}, var(--color-border-highlight))`
    : "var(--color-border-highlight)";

  return (
    <PuzzleConfigContext.Provider value={value}>
      <DndContext
        onDragStart={handleDragStart}
        onDragEnd={handleDragEnd}
        collisionDetection={rectIntersection}
      >
        {children}
        <DragOverlay>
          {activeDrag ? (
            <div
              className="selector-item"
              style={{
                ...maskStyle("0000"),
                cursor: "grabbing",
                boxShadow: "0 8px 24px rgba(0, 0, 0, 0.3)",
                background: overlayBg,
              }}
            >
              <div className="selector-item-title">{activeDrag.label}</div>
            </div>
          ) : null}
        </DragOverlay>
      </DndContext>
    </PuzzleConfigContext.Provider>
  );
}