import { useDraggable } from "@dnd-kit/core";
import "./PuzzlePiece.css";
import { maskStyle } from "../../util/puzzleMasks.js";

export default function PuzzlePiece({ id, label, type, onHover, onLeave }) {
    const { setNodeRef, listeners, attributes, isDragging } = useDraggable({
        id,
        data: { label, type },
    });

    const maskKey = "hole_tab";

    return (
        <div
            ref={setNodeRef}
            className={`puzzle-piece ${isDragging ? "dragging" : ""}`}
            style={maskStyle(maskKey)}
            {...listeners}
            {...attributes}
            onMouseEnter={() => onHover?.(type, id)}
            onMouseLeave={() => onLeave?.()}
        >
            <div className="puzzle-piece-title">{label}</div>
        </div>
    );
}
