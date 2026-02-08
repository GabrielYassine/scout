import { useDraggable } from "@dnd-kit/core";
import "../puzzlePiece/PuzzlePiece.css";
import { maskStyle } from "../../util/puzzleMasks.js";

export default function DroppedPiece({ id, label, type, index, onRemove, onHover, onLeave }) {
    const { setNodeRef, listeners, attributes, isDragging } = useDraggable({
        id: `dropped-${type}-${index}-${id}`,
        data: {
            label,
            type,
            originalId: id,
            fromType: type,
            fromIndex: index,
        },
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
