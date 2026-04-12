import { useDraggable } from "@dnd-kit/core";
import "./PuzzlePiece.css";
import { maskStyle } from "@/util/puzzleMasks.js";

export default function PuzzlePiece({
    id,
    label,
    type,
    onHover,
    onLeave,
    isDisabled = false,
    disabledReason = null,
}) {
    const { setNodeRef, listeners, attributes, isDragging } = useDraggable({
        id,
        data: { label, type, disabledReason },
        disabled: isDisabled,
    });

    const handleMouseEnter = () => {
        onHover?.(type, id, { isDisabled, disabledReason, label });
    };

    return (
        <div
            ref={setNodeRef}
            className={`selector-item-wrapper ${isDragging ? "dragging" : ""} ${isDisabled ? "disabled" : ""}`}
            {...(isDisabled ? {} : listeners)}
            {...attributes}
            onMouseEnter={handleMouseEnter}
            onMouseLeave={() => onLeave?.()}
            data-disabled-reason={isDisabled && disabledReason ? disabledReason : ""}
            aria-disabled={isDisabled}
            title={isDisabled ? disabledReason : ""}
        >
            <div
                className="selector-item"
                style={{
                    ...maskStyle("0000"),
                    background: `var(--color-${type}, var(--color-border-highlight))`,
                    opacity: isDisabled ? 0.3 : 1,
                    cursor: isDisabled ? "not-allowed" : "grab",
                }}
            >
                <div className="selector-item-title">{label}</div>
            </div>
        </div>
    );
}