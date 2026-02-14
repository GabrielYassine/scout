import { useDraggable } from "@dnd-kit/core";
import "./PuzzlePiece.css";
import { maskStyle } from "../../util/puzzleMasks.js";

export default function PuzzlePiece({ id, label, type, onHover, onLeave, isDisabled = false }) {
    const { setNodeRef, listeners, attributes, isDragging } = useDraggable({
        id,
        data: { label, type },
        disabled: isDisabled,
    });

    return (
        <div
            ref={setNodeRef}
            className={`selector-item ${isDragging ? "dragging" : ""} ${isDisabled ? "disabled" : ""}`}
            style={{
                ...maskStyle("0000"),
                background: `var(--color-${type}, var(--color-border-highlight))`,
                opacity: isDisabled ? 0.3 : 1,
                cursor: isDisabled ? 'not-allowed' : 'grab'
            }}
            {...(isDisabled ? {} : listeners)}
            {...attributes}
            onMouseEnter={() => !isDisabled && onHover?.(type, id)}
            onMouseLeave={() => onLeave?.()}
        >
            <div className="selector-item-title">{label}</div>
        </div>
    );
}
