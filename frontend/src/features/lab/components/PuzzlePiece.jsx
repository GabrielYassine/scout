/*
*  PuzzlePiece is responsible for rendering a puzzle piece,
*  handling its draggable behavior,
*  and adapting its appearance and interaction based on whether it is selectable or already placed.
*/

import { useDraggable } from "@dnd-kit/core";
import { maskStyle } from "@/shared/util/puzzleMasks.js";
import "@/features/lab/styles/PuzzlePiece.css";

export default function PuzzlePiece({
  id,
  label,
  type,
  onHover,
  onLeave,
  isDisabled = false,
  disabledReason = null,
  mode = "selector",
  index = null,
  puzzleData = null,
}) {
  const isPlaced = mode === "placed";
  // Use a unique drag id for placed pieces so they do not clash with selector pieces
  const dragId = isPlaced ? `dropped-${index}-${type}-${id}` : id;

  const dragData = isPlaced ? {  label,  type,  originalId: id, fromIndex: index, }  : { label,  type, disabledReason,  };
  // Set up the draggable behavior using useDraggable from dnd-kit
  const { setNodeRef, listeners, attributes, isDragging } = useDraggable({
    id: dragId,
    data: dragData,
    disabled: !isPlaced && isDisabled,
  });

  const { renderKey = "0000", rotation = 0, mirrorX = false } = puzzleData || {};

  const effectiveRenderKey = isPlaced ? renderKey : "0000";
  const effectiveRotation = isPlaced ? rotation : 0;
  const effectiveMirrorX = isPlaced ? mirrorX : false;

  // Compute the CSS transform for rotation and mirroring based on the piece's state
  const transform = `${effectiveMirrorX ? "scaleX(-1) " : ""}rotate(${effectiveRotation}deg)`;

  const handleMouseEnter = () => {
    if (isPlaced) {
      onHover?.(type, id);
      return;
    }

    onHover?.(type, id, { isDisabled, disabledReason, label });
  };

  return (
    <div
      ref={setNodeRef}
      className={`puzzle-piece-wrapper puzzle-piece-wrapper--${mode} ${
        isDragging ? "dragging" : ""
      } ${isDisabled ? "disabled" : ""}`}
      {...(!isDisabled ? listeners : {})}
      {...attributes}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={() => onLeave?.()}
      data-disabled-reason={!isPlaced && isDisabled && disabledReason ? disabledReason : ""}
      aria-disabled={!isPlaced && isDisabled}
      title={!isPlaced && isDisabled ? disabledReason : ""}
    >
      <div
        className="puzzle-piece-shape"
        style={{
          ...maskStyle(effectiveRenderKey),
          transform,
          transformOrigin: "center",
          background: `var(--color-${type}, var(--color-border-highlight))`,
        }}
      />

      <div className={`puzzle-piece-content puzzle-piece-content--${mode}`}>
        <div className={`puzzle-piece-title puzzle-piece-title--${mode}`}>{label}</div>
      </div>
    </div>
  );
}