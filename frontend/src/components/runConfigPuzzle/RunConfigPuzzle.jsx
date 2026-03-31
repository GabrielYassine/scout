import { useDroppable } from "@dnd-kit/core";
import { useState } from "react";
import "./RunConfigPuzzle.css";
import DroppedPiece from "./DroppedPiece.jsx";
import { usePuzzleConfig } from "../../contexts/PuzzleConfigContext.jsx";

export default function RunConfigPuzzle({ onPieceHover, onPieceLeave }) {
  const {
    configs,
    activeConfigId,
    setActiveConfigId,
    addNewConfig,
    deleteConfig,
    renameConfig,
    handleRemovePiece,
    placedPieces,
  } = usePuzzleConfig();

  const { setNodeRef, isOver } = useDroppable({
    id: "shared-drop-area",
    data: { acceptsAll: true },
  });

  const [editingTabId, setEditingTabId] = useState(null);
  const [editingName, setEditingName] = useState("");

  const handleTabClick = (configId) => {
    setActiveConfigId(configId);
  };

  const handleTabDoubleClick = (configId, currentName) => {
    setEditingTabId(configId);
    setEditingName(currentName);
  };

  const handleNameChange = (e) => {
    setEditingName(e.target.value);
  };

  const handleNameBlur = () => {
    if (editingTabId && editingName.trim()) {
      renameConfig(editingTabId, editingName.trim());
    }
    setEditingTabId(null);
    setEditingName("");
  };

  const handleNameKeyDown = (e) => {
    if (e.key === "Enter") {
      handleNameBlur();
    } else if (e.key === "Escape") {
      setEditingTabId(null);
      setEditingName("");
    }
  };

  const handleDeleteTab = (e, configId) => {
    e.stopPropagation();
    if (configs.length > 1) {
      deleteConfig(configId);
    }
  };

  return (
    <div className="run-config-puzzle">
      <div className="config-tab-buttons-row">
        {configs.map((cfg) => (
          <div
            key={cfg.id}
            className={`config-tab-button ${cfg.id === activeConfigId ? "active" : ""}`}
            onClick={() => handleTabClick(cfg.id)}
            onDoubleClick={() => handleTabDoubleClick(cfg.id, cfg.name)}
          >
            {editingTabId === cfg.id ? (
              <input
                type="text"
                className="config-tab-input"
                value={editingName}
                onChange={handleNameChange}
                onBlur={handleNameBlur}
                onKeyDown={handleNameKeyDown}
                autoFocus
                onClick={(e) => e.stopPropagation()}
              />
            ) : (
              <span className="config-tab-name">{cfg.name}</span>
            )}
            {configs.length > 1 && (
              <button
                className="config-tab-close"
                onClick={(e) => handleDeleteTab(e, cfg.id)}
                title="Delete config"
              >
                ×
              </button>
            )}
          </div>
        ))}
        <button className="config-tab-button config-tab-add" onClick={addNewConfig} title="Add new config">
          +
        </button>
      </div>

      <div
        ref={setNodeRef}
        className={`shared-drop-area ${isOver ? "drop-area-over" : ""}`}
      >
        {placedPieces.map((piece, index) => (
          <DroppedPiece
            key={`${piece.id}-${index}`}
            id={piece.id}
            label={piece.label}
            type={piece.type}
            index={index}
            puzzleData={piece.puzzleData}
            onRemove={() => handleRemovePiece(index)}
            onHover={onPieceHover}
            onLeave={onPieceLeave}
          />
        ))}
      </div>
    </div>
  );
}
