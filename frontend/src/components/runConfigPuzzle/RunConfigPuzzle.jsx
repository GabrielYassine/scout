import { useDroppable, useDraggable } from "@dnd-kit/core";
import "./RunConfigPuzzle.css";

function DroppedPiece({ id, label, zoneId }) {
    const { setNodeRef, listeners, attributes, isDragging } = useDraggable({
        id: `dropped-${zoneId}-${id}`,
        data: {
            label,
            originalId: id,
            fromZone: zoneId,
        }
    });

    return (
        <div
            ref={setNodeRef}
            className={`dropped-piece ${isDragging ? "dragging" : ""}`}
            {...listeners}
            {...attributes}
        >
            <div className="dropped-piece-content">{label}</div>
        </div>
    );
}

function DropZone({ id, label, piece }) {
    const { setNodeRef, isOver } = useDroppable({ id });

    return (
        <div
            ref={setNodeRef}
            className={`drop-zone ${isOver ? "drop-zone-over" : ""} ${piece ? "drop-zone-filled" : ""}`}
        >
            <div className="drop-zone-label">{label}</div>
            {piece ? (
                <DroppedPiece id={piece.id} label={piece.label} zoneId={id} />
            ) : (
                <div className="drop-zone-placeholder">Drop here</div>
            )}
        </div>
    );
}

export default function RunConfigPuzzle({ config }) {
    return (
        <div className="run-config-puzzle">
            <div className="puzzle-row">
                <DropZone id="searchSpace"  label="Search Space"  piece={config?.searchSpace} />
                <DropZone id="problem" label="Problem" piece={config?.problem}/>
                <DropZone id="algorithm" label="Algorithm" piece={config?.algorithm}/>
                <DropZone id="mutation" label="Mutation" piece={config?.mutation}/>
                <DropZone id="acceptance" label="Acceptance Rule" piece={config?.acceptance}/>
                <DropZone id="stopCondition" label="Stop Condition" piece={config?.stopCondition} />
            </div>
        </div>
    );
}