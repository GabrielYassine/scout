/*
 * PuzzleConfigProvider manages the shared puzzle configuration state for the lab.
 * It stores configs, params, TSP/VRP instances, and drag-and-drop state,
 * and exposes actions for updating, resetting, renaming, and applying templates.
 */
import { createContext, useMemo, useState } from "react";
import { DndContext, DragOverlay, rectIntersection } from "@dnd-kit/core";

import { useSessionStorageState } from "@/shared/hooks/useSessionStorageState.js";
import { maskStyle } from "@/shared/util/puzzleMasks.js";

import {
  cloneTspInstance,
  cloneVrpInstance,
  createDefaultConfig,
  createEmptyParams,
  deriveGroupedPuzzleConfig,
  rekeyGrid,
  applyTemplateRunRequestToState,
  createFallbackConfig,
  normalizeConfigList,
  getNextConfigName,
  DEFAULT_CONFIG_ID,
} from "@/shared/contexts/puzzleConfigHelpers.js";

import "@/features/lab/components/PuzzlePiece.css";

export const PuzzleConfigContext = createContext(null);



export function PuzzleConfigProvider({ children }) {
  const [configs, setConfigs] = useSessionStorageState("scout:runConfigs", [
    createFallbackConfig(),
  ]);

  const normalizedConfigs = useMemo(
    () => normalizeConfigList(configs, true),
    [configs]
  );

  const [activeConfigId, setActiveConfigId] = useSessionStorageState(
    "scout:activeConfigId",
    DEFAULT_CONFIG_ID
  );

  const [activeDrag, setActiveDrag] = useState(null);

  const activeConfig = useMemo(
    () => normalizedConfigs.find((config) => config.id === activeConfigId) ?? normalizedConfigs[0],
    [normalizedConfigs, activeConfigId]
  );

  const placedPieces = activeConfig?.placedPieces ?? [];
  const puzzleConfig = useMemo(() => deriveGroupedPuzzleConfig(placedPieces), [placedPieces]);
  const params = activeConfig?.params ?? createEmptyParams();
  const tspInstance = activeConfig?.tspInstance ? cloneTspInstance(activeConfig.tspInstance) : null;
  const vrpInstance = activeConfig?.vrpInstance ? cloneVrpInstance(activeConfig.vrpInstance) : null;

  function updateActiveConfig(key, updater) {
    setConfigs((prev) => {
      const safePrev = normalizeConfigList(prev, true);

      return safePrev.map((config) =>
        config.id === activeConfigId
          ? {
              ...config,
              [key]: typeof updater === "function" ? updater(config[key]) : updater,
            }
          : config
      );
    });
  }

  const setPlacedPieces = (updater) => updateActiveConfig("placedPieces", updater);
  const setParams = (updater) => updateActiveConfig("params", updater);
  const setTspInstance = (updater) => updateActiveConfig("tspInstance", updater);
  const setVrpInstance = (updater) => updateActiveConfig("vrpInstance", updater);

  function addNewConfig() {
    const newId = `config-${Date.now()}`;

    setConfigs((prev) => {
      const safePrev = normalizeConfigList(prev, false);
      const newConfig = createDefaultConfig(newId, getNextConfigName(safePrev));
      return [...safePrev, newConfig];
    });

    setActiveConfigId(newId);
  }

  function deleteConfig(configId) {
    setConfigs((prev) => {
      const safePrev = normalizeConfigList(prev, false);
      if (safePrev.length <= 1) return safePrev;

      const next = safePrev.filter((config) => config.id !== configId);

      if (activeConfigId === configId) {
        setActiveConfigId(next[0]?.id ?? DEFAULT_CONFIG_ID);
      }

      return next;
    });
  }

  function renameConfig(configId, newName) {
    setConfigs((prev) => {
      const safePrev = normalizeConfigList(prev, false);

      return safePrev.map((config) =>
        config.id === configId ? { ...config, name: newName } : config
      );
    });
  }

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
      return rekeyGrid(next);
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

      return rekeyGrid(nextPieces);
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
              className="puzzle-piece-wrapper puzzle-piece-wrapper--selector"
              style={{
                "--puzzle-piece-size": "clamp(6rem, 12vw, var(--puzzle-piece-max-size))",
              }}
            >
              <div
                className="puzzle-piece-shape"
                style={{
                  ...maskStyle("0000"),
                  background: overlayBg,
                  cursor: "grabbing",
                  boxShadow: "0 8px 24px rgba(0, 0, 0, 0.3)",
                }}
              />
              <div className="puzzle-piece-content puzzle-piece-content--selector">
                <div className="puzzle-piece-title puzzle-piece-title--selector">
                  {activeDrag.label}
                </div>
              </div>
            </div>
          ) : null}
        </DragOverlay>
      </DndContext>
    </PuzzleConfigContext.Provider>
  );
}