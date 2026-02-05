import { useDraggable } from "@dnd-kit/core";
import "./PuzzlePiece.css";

export default function PuzzlePiece({ id, label, type }) {
    const { setNodeRef, listeners, attributes, isDragging } =
        useDraggable({
            id,
            data: {
                label,
                type,
            }
        });

    return (
        <div
            ref={setNodeRef}
            className={`puzzle-piece ${isDragging ? "dragging" : ""}`}
            {...listeners}
            {...attributes}
        >
            <div className="puzzle-piece-title">{label}</div>
        </div>
    );
}
