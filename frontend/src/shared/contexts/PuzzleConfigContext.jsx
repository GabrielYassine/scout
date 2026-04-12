import { useMemo, useState } from "react";
import { DndContext, DragOverlay, rectIntersection } from "@dnd-kit/core";

import { PuzzleConfigContext } from "@/shared/contexts/PuzzleConfigContextInternal.js";
import { useSessionStorageState } from "@/shared/hooks/useSessionStorageState.js";
import { maskStyle } from "@/shared/util/puzzleMasks.js";

import {
  GRID_COLUMNS,
  cloneTspInstance,
  cloneVrpInstance,
  createDefaultConfig,
  createEmptyParams,
  deriveGroupedPuzzleConfig,
  normalizeStoredConfig,
  rekeyGrid,
  applyTemplateRunRequestToState,
} from "@/shared/contexts/puzzleConfigHelpers.js";

import "@/features/lab/components/selector/PuzzlePiece.css";

export function PuzzleConfigProvider({ children }) {
  const [configs, setConfigs] = useSessionStorageState("scout:runConfigs", [
    createDefaultConfig("config-1", "Config 1"),
  ]);

  const normalizedConfigs = useMemo(
    () =>
      Array.isArray(configs)
        ? configs.map(normalizeStoredConfig)
        : [createDefaultConfig("config-1", "Config 1")],
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
  const tspInstance = activeConfig?.tspInstance ? cloneTspInstance(activeConfig.tspInstance) : null;
  const vrpInstance = activeConfig?.vrpInstance ? cloneVrpInstance(activeConfig.vrpInstance) : null;

  const updateActiveConfig = (key, updater) => {
    setConfigs((prev) => {
      const safePrev = Array.isArray(prev)
        ? prev.map(normalizeStoredConfig)
        : [createDefaultConfig("config-1", "Config 1")];
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

      const switchedToRuntimeStudy = previousMode !== "runtimeStudy" && nextMode === "runtimeStudy";

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
    applyTemplateRunRequestToState({ runRequest, catalog, setPlacedPieces, setParams });
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
              className="selector-item-wrapper selector-item-wrapper--overlay"
              style={{
                "--puzzle-piece-size": "clamp(6rem, 12vw, var(--puzzle-piece-max-size))",
                "--overlay-bg": overlayBg,
              }}
            >
              <div
                className="selector-item selector-item--overlay"
                style={{
                  ...maskStyle("0000"),
                  cursor: "grabbing",
                }}
              >
                <div className="selector-item-title">{activeDrag.label}</div>
              </div>
            </div>
          ) : null}
        </DragOverlay>
      </DndContext>
    </PuzzleConfigContext.Provider>
  );
}
