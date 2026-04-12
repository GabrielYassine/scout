import { createContext, useContext, useMemo, useState } from "react";
import { DndContext, DragOverlay, rectIntersection } from "@dnd-kit/core";

import { useSessionStorageState } from "@/shared/hooks/useSessionStorageState.js";
import { generatePuzzleKey } from "@/shared/util/puzzleGenerator.js";
import { maskStyle } from "@/shared/util/puzzleMasks.js";

import "@/features/lab/components/selector/PuzzlePiece.css";

const PuzzleConfigContext = createContext(null);
const GRID_COLUMNS = 6;

export const usePuzzleConfig = () => {
  const context = useContext(PuzzleConfigContext);
  if (!context) throw new Error("usePuzzleConfig must be used within PuzzleConfigProvider");
  return context;
};

const componentTypes = [
  "searchSpace",
  "problem",
  "generator",
  "selection",
  "populationModel",
  "parentSelectionRule",
  "crossover",
  "stopCondition",
  "observer",
];

const cloneTspInstance = (tsp) =>
  tsp
    ? {
        ...tsp,
        cities: (tsp.cities ?? []).map((c) => ({ ...c })),
      }
    : null;

const cloneVrpInstance = (vrp) =>
  vrp
    ? {
        ...vrp,
        depot: vrp.depot ? { ...vrp.depot } : null,
        customers: (vrp.customers ?? []).map((c) => ({ ...c })),
      }
    : null;

const createEmptyPuzzleConfig = () => ({
  searchSpace: [],
  problem: [],
  generator: [],
  selection: [],
  populationModel: [],
  parentSelectionRule: [],
  crossover: [],
  stopCondition: [],
  observer: [],
});

const createEmptyParams = () => ({
  global: {},
  searchSpace: {},
  problem: {},
  generator: {},
  selection: {},
  populationModel: {},
  parentSelectionRule: {},
  crossover: {},
  stopCondition: {},
  observer: {},
});

const createDefaultConfig = (id, name) => ({
  id,
  name,
  placedPieces: [],
  params: createEmptyParams(),
  tspInstance: null,
  vrpInstance: null,
});

function flattenGroupedPuzzleConfig(groupedConfig) {
  const grouped = groupedConfig ?? createEmptyPuzzleConfig();
  return componentTypes.flatMap((type) =>
    (Array.isArray(grouped[type]) ? grouped[type] : []).map((piece) => ({
      id: piece.id,
      label: piece.label,
      type: piece.type ?? type,
      puzzleData: piece.puzzleData,
    }))
  );
}

function normalizeStoredConfig(config, fallbackIndex = 0) {
  const base = {
    id: config?.id ?? `config-${fallbackIndex + 1}`,
    name: config?.name ?? `Config ${fallbackIndex + 1}`,
    params: config?.params ?? createEmptyParams(),
    tspInstance: config?.tspInstance ? cloneTspInstance(config.tspInstance) : null,
    vrpInstance: config?.vrpInstance ? cloneVrpInstance(config.vrpInstance) : null,
  };

  if (Array.isArray(config?.placedPieces)) {
    return {
      ...base,
      placedPieces: config.placedPieces.map((piece) => ({
        id: piece.id,
        label: piece.label,
        type: piece.type,
        puzzleData: piece.puzzleData,
      })),
    };
  }

  return {
    ...base,
    placedPieces: flattenGroupedPuzzleConfig(config?.puzzleConfig),
  };
}

function getEdge(piece, direction) {
  const key = piece?.puzzleData?.logicalKey;
  if (!key) return null;

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
}

function buildGridNeighbors(pieces, index, totalCols = GRID_COLUMNS) {
  const col = index % totalCols;
  const row = Math.floor(index / totalCols);

  const leftIndex = col > 0 ? index - 1 : null;
  const rightIndex = col < totalCols - 1 ? index + 1 : null;
  const topIndex = row > 0 ? index - totalCols : null;
  const bottomIndex = index + totalCols < pieces.length ? index + totalCols : null;

  return {
    left:
      leftIndex === null
        ? { kind: "wall" }
        : pieces[leftIndex]
        ? { kind: "piece", edge: getEdge(pieces[leftIndex], "E") }
        : { kind: "empty" },

    right:
      rightIndex === null
        ? { kind: "wall" }
        : pieces[rightIndex]
        ? { kind: "piece", edge: getEdge(pieces[rightIndex], "W") }
        : { kind: "empty" },

    top:
      topIndex === null
        ? { kind: "wall" }
        : pieces[topIndex]
        ? { kind: "piece", edge: getEdge(pieces[topIndex], "S") }
        : { kind: "empty" },

    bottom:
      bottomIndex === null
        ? { kind: "empty" }
        : pieces[bottomIndex]
        ? { kind: "piece", edge: getEdge(pieces[bottomIndex], "N") }
        : { kind: "empty" },
  };
}

function rekeyGrid(pieces, startIndex = 0, totalCols = GRID_COLUMNS) {
  const next = Array.isArray(pieces) ? [...pieces] : [];

  for (let i = Math.max(0, startIndex); i < next.length; i++) {
    const col = i % totalCols;
    const row = Math.floor(i / totalCols);
    const neighbors = buildGridNeighbors(next, i, totalCols);

    next[i] = {
      ...next[i],
      puzzleData: generatePuzzleKey({
        col,
        row,
        totalCols,
        neighbors,
      }),
    };
  }

  return next;
}

function deriveGroupedPuzzleConfig(placedPieces) {
  const grouped = createEmptyPuzzleConfig();
  const pieces = Array.isArray(placedPieces) ? placedPieces : [];

  for (const piece of pieces) {
    if (!piece?.type || !grouped[piece.type]) continue;
    grouped[piece.type].push(piece);
  }

  return grouped;
}

export function PuzzleConfigProvider({ children }) {
  const [configs, setConfigs] = useSessionStorageState("scout:runConfigs", [
    createDefaultConfig("config-1", "Config 1"),
  ]);

  const normalizedConfigs = useMemo(
    () => (Array.isArray(configs) ? configs.map(normalizeStoredConfig) : [createDefaultConfig("config-1", "Config 1")]),
    [configs]
  );

  const [activeConfigId, setActiveConfigId] = useSessionStorageState(
    "scout:activeConfigId",
    "config-1"
  );

  const [activeDrag, setActiveDrag] = useState(null);

  const activeConfig = useMemo(
    () => normalizedConfigs.find((c) => c.id === activeConfigId) || normalizedConfigs[0],
    [normalizedConfigs, activeConfigId]
  );

  const placedPieces = activeConfig?.placedPieces ?? [];
  const puzzleConfig = useMemo(() => deriveGroupedPuzzleConfig(placedPieces), [placedPieces]);
  const params = activeConfig?.params ?? createEmptyParams();
  const tspInstance = activeConfig?.tspInstance
    ? cloneTspInstance(activeConfig.tspInstance)
    : null;
  const vrpInstance = activeConfig?.vrpInstance
    ? cloneVrpInstance(activeConfig.vrpInstance)
    : null;

  const updateActiveConfig = (key, updater) => {
    setConfigs((prev) => {
      const safePrev = Array.isArray(prev) ? prev.map(normalizeStoredConfig) : [createDefaultConfig("config-1", "Config 1")];
      return safePrev.map((config) =>
        config.id === activeConfigId
          ? { ...config, [key]: typeof updater === "function" ? updater(config[key]) : updater }
          : config
      );
    });
  };

  const setPlacedPieces = (updater) => updateActiveConfig("placedPieces", updater);
  const setParams = (updater) => updateActiveConfig("params", updater);
  const setTspInstance = (updater) => updateActiveConfig("tspInstance", updater);
  const setVrpInstance = (updater) => updateActiveConfig("vrpInstance", updater);

  const addNewConfig = () => {
    const newId = `config-${Date.now()}`;
    const newConfig = createDefaultConfig(newId, `Config ${normalizedConfigs.length + 1}`);
    setConfigs((prev) => [...(Array.isArray(prev) ? prev.map(normalizeStoredConfig) : []), newConfig]);
    setActiveConfigId(newId);
  };

  const deleteConfig = (configId) => {
    setConfigs((prev) => {
      const safePrev = Array.isArray(prev) ? prev.map(normalizeStoredConfig) : [];
      if (safePrev.length === 1) return safePrev;

      const next = safePrev.filter((c) => c.id !== configId);
      if (activeConfigId === configId) {
        setActiveConfigId(next[0]?.id ?? "config-1");
      }
      return next;
    });
  };

  const renameConfig = (configId, newName) => {
    setConfigs((prev) => {
      const safePrev = Array.isArray(prev) ? prev.map(normalizeStoredConfig) : [];
      return safePrev.map((c) => (c.id === configId ? { ...c, name: newName } : c));
    });
  };

  function handleDragStart({ active }) {
    setActiveDrag({
      id: active.id,
      label: active.data?.current?.label || active.id,
      type: active.data?.current?.type || null,
    });
  }

  function handleRemovePiece(typeOrIndex, maybeIndex) {
    const index = typeof maybeIndex === "number" ? maybeIndex : typeOrIndex;
    if (typeof index !== "number") return;

    setPlacedPieces((prev) => {
      const next = (Array.isArray(prev) ? prev : []).filter((_, i) => i !== index);
      return rekeyGrid(next, Math.max(0, index - GRID_COLUMNS));
    });
  }

  function handleDragEnd({ active, over }) {
    setActiveDrag(null);

    if (!over) {
      if (active.id.toString().startsWith("dropped-")) {
        const { fromIndex } = active.data?.current ?? {};
        if (fromIndex != null) {
          handleRemovePiece(fromIndex);
        }
      }
      return;
    }

    if (over.id !== "shared-drop-area") return;

    const pieceType = active.data?.current?.type;
    if (!pieceType) return;
    if (active.id.toString().startsWith("dropped-")) return;

    setPlacedPieces((prev) => {
      const currentPieces = Array.isArray(prev) ? prev : [];
      const nextPieces = [
        ...currentPieces,
        {
          id: active.id,
          label: active.data?.current?.label || active.id,
          type: pieceType,
        },
      ];
      return rekeyGrid(nextPieces, nextPieces.length - 1);
    });
  }

function handleParamChange(type, newParams) {
  if (type === "global") {
    const previousMode = params?.global?.experimentType ?? "run";
    const nextMode = newParams?.experimentType ?? previousMode;

    const switchedToRuntimeStudy =
      previousMode !== "runtimeStudy" && nextMode === "runtimeStudy";

    if (switchedToRuntimeStudy) {
      setPlacedPieces([]);
      setParams((prev) => ({
        ...prev,
        global: newParams,
        searchSpace: {},
        problem: {},
        generator: {},
        selection: {},
        populationModel: {},
        parentSelectionRule: {},
        crossover: {},
        stopCondition: {},
        observer: {},
      }));
      return;
    }
  }

  setParams((prev) => ({ ...prev, [type]: newParams }));
}

  function handleReset() {
    setPlacedPieces([]);
    setParams(createEmptyParams());
  }

  function applyTemplateRunRequest(runRequest, catalog) {
    if (!runRequest || !catalog) return;

    const normalizeIds = (value, single) => {
      if (value == null) return [];
      const ids = Array.isArray(value) ? value : [value];
      return single ? ids.slice(0, 1) : ids;
    };

    const mapIdsToPieces = (ids, catalogItems, pieceType, single = false) =>
      normalizeIds(ids, single)
        .map((id) => {
          const item = catalogItems.find((x) => x.id === id);
          return item ? { id: item.id, label: item.displayName, type: pieceType } : null;
        })
        .filter(Boolean);

    const componentMapping = [
      { type: "searchSpace", catalogKey: "searchSpaces", requestKey: "searchSpaceId", single: true },
      { type: "problem", catalogKey: "problems", requestKey: "problemIds" },
      { type: "generator", catalogKey: "generators", requestKey: "generatorId", single: true },
      { type: "selection", catalogKey: "selectionRules", requestKey: "selectionRuleId", single: true },
      { type: "populationModel", catalogKey: "populationModels", requestKey: "populationModelId", single: true },
      { type: "parentSelectionRule", catalogKey: "parentSelectionRules", requestKey: "parentSelectionRuleId", single: true },
      { type: "crossover", catalogKey: "crossovers", requestKey: "crossoverId", single: true },
      { type: "stopCondition", catalogKey: "stopConditions", requestKey: "stopConditionIds" },
      { type: "observer", catalogKey: "observers", requestKey: "observerIds" },
    ];

    const flattenedPieces = componentMapping.flatMap(({ type, catalogKey, requestKey, single }) =>
      catalog[catalogKey] ? mapIdsToPieces(runRequest[requestKey], catalog[catalogKey], type, single) : []
    );

    setPlacedPieces(rekeyGrid(flattenedPieces, 0));

    setParams({
      global: {
        experimentType: "run",
        seed: runRequest.seed || Date.now(),
        runTimes: runRequest.runTimes || 1,
        logEveryIterations: runRequest.logEveryIterations || 100,
        wsUpdateEveryIterations: runRequest.wsUpdateEveryIterations || 100,
        problemSizes: "100, 200, 400, 800",
        repetitionsPerSize: 10,
        wsUpdateEverySizes: 1,
      },
      searchSpace: runRequest.searchSpaceParams || {},
      problem: runRequest.problemParams || {},
      generator: runRequest.generatorParams || {},
      selection: runRequest.selectionRuleParams || {},
      populationModel: runRequest.populationModelParams || {},
      parentSelectionRule: runRequest.parentSelectionRuleParams || {},
      crossover: runRequest.crossoverParams || {},
      stopCondition: runRequest.stopConditionParams || {},
      observer: {},
    });
  }

  const value = {
    configs: normalizedConfigs,
    activeConfigId,
    activeConfig,
    puzzleConfig,
    placedPieces,
    params,
    tspInstance,
    vrpInstance,

    setActiveConfigId,
    addNewConfig,
    deleteConfig,
    renameConfig,

    handleRemovePiece,
    handleParamChange,
    handleReset,
    applyTemplateRunRequest,

    setTspInstance,
    setVrpInstance,

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
