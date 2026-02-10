import { createContext, useContext, useState } from "react";
import { DndContext, DragOverlay, rectIntersection } from "@dnd-kit/core";
import { useSessionStorageState } from "../hooks/useSessionStorageState.js";
import { generatePuzzleKey } from "../util/puzzleGenerator.js";
import { maskStyle } from "../util/puzzleMasks.js";
import "../components/puzzlePiece/PuzzlePiece.css";

const PuzzleConfigContext = createContext(null);

export const usePuzzleConfig = () => {
    const context = useContext(PuzzleConfigContext);
    if (!context) {
        throw new Error("usePuzzleConfig must be used within PuzzleConfigProvider");
    }
    return context;
};

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

export function PuzzleConfigProvider({ children }) {
    const [configs, setConfigs] = useSessionStorageState(
        "scout:runConfigs",
        [
            {
                id: "config-1",
                name: "Config 1",
                puzzleConfig: {
                    searchSpace: [],
                    problem: [],
                    algorithm: [],
                    mutation: [],
                    acceptance: [],
                    populationModel: [],
                    stopCondition: [],
                    observer: [],
                },
                params: {
                    searchSpace: {},
                    problem: {},
                    algorithm: {},
                    mutation: {},
                    acceptance: {},
                    populationModel: {},
                    stopCondition: {},
                    observer: {},
                }
            }
        ]
    );

    const [activeConfigId, setActiveConfigId] = useSessionStorageState(
        "scout:activeConfigId",
        "config-1"
    );

    const [activeId, setActiveId] = useState(null);
    const [activeLabel, setActiveLabel] = useState(null);
    const [activeType, setActiveType] = useState(null);

    // Helper to get active config
    const activeConfig = configs.find(c => c.id === activeConfigId) || configs[0];
    const puzzleConfig = activeConfig.puzzleConfig;
    const params = activeConfig.params;

    // Helper to update active config's puzzleConfig
    const setPuzzleConfig = (updater) => {
        setConfigs(prev => prev.map(config =>
            config.id === activeConfigId
                ? { ...config, puzzleConfig: typeof updater === 'function' ? updater(config.puzzleConfig) : updater }
                : config
        ));
    };

    // Helper to update active config's params
    const setParams = (updater) => {
        setConfigs(prev => prev.map(config =>
            config.id === activeConfigId
                ? { ...config, params: typeof updater === 'function' ? updater(config.params) : updater }
                : config
        ));
    };

    // Add a new config
    const addNewConfig = () => {
        const newId = `config-${Date.now()}`;
        const newConfig = {
            id: newId,
            name: `Config ${configs.length + 1}`,
            puzzleConfig: {
                searchSpace: [],
                problem: [],
                algorithm: [],
                mutation: [],
                acceptance: [],
                populationModel: [],
                stopCondition: [],
                observer: [],
            },
            params: {
                searchSpace: {},
                problem: {},
                algorithm: {},
                mutation: {},
                acceptance: {},
                populationModel: {},
                stopCondition: {},
                observer: {},
            }
        };
        setConfigs(prev => [...prev, newConfig]);
        setActiveConfigId(newId);
    };

    // Delete a config
    const deleteConfig = (configId) => {
        if (configs.length === 1) return;
        setConfigs(prev => prev.filter(c => c.id !== configId));
        if (activeConfigId === configId) {
            setActiveConfigId(configs[0].id === configId ? configs[1].id : configs[0].id);
        }
    };

    // Rename a config
    const renameConfig = (configId, newName) => {
        setConfigs(prev => prev.map(config =>
            config.id === configId ? { ...config, name: newName } : config
        ));
    };

    // Puzzle key generation helpers
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

    // DnD Handlers
    function handleDragStart(event) {
        const { active } = event;
        setActiveId(active.id);
        setActiveLabel(active.data?.current?.label || active.id);
        setActiveType(active.data?.current?.type || null);
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
        setActiveType(null);

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

        if (over.id !== "shared-drop-area") return;

        const pieceType = active.data?.current?.type;
        if (!pieceType) return;

        // MOVE existing piece
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

                nextConfig = rekeyColumn(nextConfig, fromType, fromIndex);
                nextConfig = rekeyColumn(nextConfig, pieceType, newTo.length - 1);

                return nextConfig;
            });

            return;
        }

        // ADD new piece
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

    const value = {
        // Config management
        configs,
        activeConfigId,
        activeConfig,
        puzzleConfig,
        params,
        setActiveConfigId,
        addNewConfig,
        deleteConfig,
        renameConfig,

        // Puzzle manipulation
        handleRemovePiece,
        handleParamChange,
        handleReset,

        // DnD state
        activeId,
        activeLabel,
    };

    return (
        <PuzzleConfigContext.Provider value={value}>
            <DndContext
                onDragStart={handleDragStart}
                onDragEnd={handleDragEnd}
                collisionDetection={rectIntersection}
            >
                {children}
                <DragOverlay>
                    {activeId ? (
                        <div
                            className="selector-item"
                            style={{
                                ...maskStyle("0000"),
                                cursor: 'grabbing',
                                boxShadow: '0 8px 24px rgba(0, 0, 0, 0.3)',
                                background: activeType ? `var(--color-${activeType}, var(--color-border-highlight))` : 'var(--color-border-highlight)'
                            }}
                        >
                            <div className="selector-item-title">{activeLabel}</div>
                        </div>
                    ) : null}
                </DragOverlay>
            </DndContext>
        </PuzzleConfigContext.Provider>
    );
}
