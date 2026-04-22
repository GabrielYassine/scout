import { useDraggable } from "@dnd-kit/core";
import { maskStyle } from "@/shared/util/puzzleMasks.js";
import "./PuzzlePiece.css";

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

  const dragId = isPlaced ? `dropped-${index}-${type}-${id}` : id;

  const dragData = isPlaced
    ? {
        label,
        type,
        originalId: id,
        fromIndex: index,
      }
    : {
        label,
        type,
        disabledReason,
      };

  const { setNodeRef, listeners, attributes, isDragging } = useDraggable({
    id: dragId,
    data: dragData,
    disabled: !isPlaced && isDisabled,
  });

  const { renderKey = "0000", rotation = 0, mirrorX = false } = puzzleData || {};

  const effectiveRenderKey = isPlaced ? renderKey : "0000";
  const effectiveRotation = isPlaced ? rotation : 0;
  const effectiveMirrorX = isPlaced ? mirrorX : false;

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
          opacity: !isPlaced && isDisabled ? 0.58 : 1,
          cursor: !isPlaced && isDisabled ? "not-allowed" : "grab",
        }}
      />

      <div className={`puzzle-piece-content puzzle-piece-content--${mode}`}>
        <div className={`puzzle-piece-title puzzle-piece-title--${mode}`}>{label}</div>
      </div>
    </div>
  );
}