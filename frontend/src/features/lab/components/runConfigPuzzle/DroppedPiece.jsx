import { useDraggable } from "@dnd-kit/core";

import { maskStyle } from "@/shared/util/puzzleMasks.js";

import "./DroppedPiece.css";

export default function DroppedPiece({ id, label, type, index, puzzleData, onHover, onLeave }) {
  const { setNodeRef, listeners, attributes, isDragging } = useDraggable({
    id: `dropped-${index}-${type}-${id}`,
    data: {
      label,
      type,
      originalId: id,
      fromIndex: index,
    },
  });

  const { renderKey = "0000", rotation = 0, mirrorX = false } = puzzleData || {};
  const transform = `${mirrorX ? "scaleX(-1) " : ""}rotate(${rotation}deg)`;

  return (
    <div
      ref={setNodeRef}
      className={`dropped-puzzle-piece ${isDragging ? "dragging" : ""}`}
      {...listeners}
      {...attributes}
      onMouseEnter={() => onHover?.(type, id)}
      onMouseLeave={() => onLeave?.()}
    >
      <div
        className="dropped-puzzle-shape"
        style={{
          ...maskStyle(renderKey),
          transform,
          transformOrigin: "center",
          background: `var(--color-${type}, var(--color-border-highlight))`,
        }}
      />
      <div className="dropped-puzzle-content">
        <div className="dropped-puzzle-piece-title">{label}</div>
      </div>
    </div>
  );
}
