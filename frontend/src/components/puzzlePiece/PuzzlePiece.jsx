import { useDraggable } from "@dnd-kit/core";
import "./PuzzlePiece.css";
import { maskStyle } from "../../util/puzzleMasks.js";

export default function PuzzlePiece({ id, label, type, onHover, onLeave }) {
    const { setNodeRef, listeners, attributes, isDragging } = useDraggable({
        id,
        data: { label, type },
    });

    return (
        <div
            ref={setNodeRef}
            className={`selector-item ${isDragging ? "dragging" : ""}`}
            style={{...maskStyle("0000"), background: `var(--color-${type}, var(--color-border-highlight))`}}
            {...listeners}
            {...attributes}
            onMouseEnter={() => onHover?.(type, id)}
            onMouseLeave={() => onLeave?.()}
        >
            <div className="selector-item-title">{label}</div>
        </div>
    );
}
