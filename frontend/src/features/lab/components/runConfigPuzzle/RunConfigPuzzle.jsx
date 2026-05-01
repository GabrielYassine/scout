/*
* RunConfigPuzzle is responsible for showing the current puzzle configuration,
*  handling the config tabs,
* and rendering the shared drop area with the placed puzzle pieces.
*/
import { useDroppable } from "@dnd-kit/core";
import { useState } from "react";

import PuzzlePiece from "../PuzzlePiece.jsx";
import { usePuzzleConfig } from "@/shared/contexts/usePuzzleConfig.js";

import "@/features/lab/styles/RunConfigPuzzle.css";

export default function RunConfigPuzzle({ onPieceHover, onPieceLeave }) {

  const {
    configs,
    activeConfigId,
    setActiveConfigId,
    addNewConfig,
    deleteConfig,
    renameConfig,
    placedPieces,
  } = usePuzzleConfig();

 // Set up the droppable area for placing puzzle pieces.
  const { setNodeRef, isOver } = useDroppable({
    id: "shared-drop-area",
    data: { acceptsAll: true },
  });

  const [editingTabId, setEditingTabId] = useState(null);
  const [editingName, setEditingName] = useState("");

  const startEditingTab = (configId, currentName) => {
    setEditingTabId(configId);
    setEditingName(currentName);
  };

  const stopEditingTab = () => {
    setEditingTabId(null);
    setEditingName("");
  };

  const commitTabName = () => {
    if (editingTabId && editingName.trim()) {
      renameConfig(editingTabId, editingName.trim());
    }
    stopEditingTab();
  };

  const handleDeleteTab = (event, configId) => {
    event.stopPropagation();
    if (configs.length > 1) {
      deleteConfig(configId);
    }
  };

  return (
    <div className="run-config-puzzle">
      <div className="config-tab-buttons-row">
        {configs.map((config) => (
          <div
            key={config.id}
            className={`config-tab-button ${config.id === activeConfigId ? "active" : ""}`}
            onClick={() => setActiveConfigId(config.id)}
            onDoubleClick={() => startEditingTab(config.id, config.name)}
          >
            {editingTabId === config.id ? (
              <input
                type="text"
                className="config-tab-input"
                value={editingName}
                onChange={(event) => setEditingName(event.target.value)}
                onBlur={commitTabName}
                onKeyDown={(event) => {
                  if (event.key === "Enter") {
                    commitTabName();
                  } else if (event.key === "Escape") {
                    stopEditingTab();
                  }
                }}
                autoFocus
                onClick={(event) => event.stopPropagation()}
              />
            ) : (
              <span className="config-tab-name">{config.name}</span>
            )}

            {configs.length > 1 && (
              <button
                className="config-tab-close"
                onClick={(event) => handleDeleteTab(event, config.id)}
                title="Delete config"
              >
                ×
              </button>
            )}
          </div>
        ))}

        <button
          className="config-tab-button config-tab-add"
          onClick={addNewConfig}
          title="Add new config"
        >
          +
        </button>
      </div>

      <div
        ref={setNodeRef}
        className={`shared-drop-area ${isOver ? "drop-area-over" : ""}`}
      >
        {placedPieces.map((piece, index) => (
          <PuzzlePiece
            key={`${piece.id}-${index}`}
            id={piece.id}
            label={piece.label}
            type={piece.type}
            index={index}
            puzzleData={piece.puzzleData}
            mode="placed"
            onHover={onPieceHover}
            onLeave={onPieceLeave}
          />
        ))}
      </div>
    </div>
  );
}