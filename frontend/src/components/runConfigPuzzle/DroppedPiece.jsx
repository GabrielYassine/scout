import { useDraggable } from "@dnd-kit/core";
import "../puzzlePiece/PuzzlePiece.css";

export default function DroppedPiece({ id, label, type, index, onRemove }) {
    const { setNodeRef, listeners, attributes, isDragging } = useDraggable({
        id: `dropped-${type}-${index}-${id}`,
        data: {
            label,
            type,
            originalId: id,
            fromType: type,
            fromIndex: index,
        }
    });

    return (
        <div ref={setNodeRef} className={`puzzle-piece ${isDragging ? "dragging" : ""}`}
            {...listeners}
            {...attributes}
        >
            <div className="puzzle-piece-title">{label}</div>
        </div>
    );
}
