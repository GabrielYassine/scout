import { useDroppable, useDraggable } from "@dnd-kit/core";
import "./RunConfigPuzzle.css";

function DroppedPiece({ id, label, type, zoneId }) {
    const { setNodeRef, listeners, attributes, isDragging } = useDraggable({
        id: `dropped-${zoneId}-${id}`,
        data: {
            label,
            type,
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

function DropZone({ id, label, acceptsType, piece }) {
    const { setNodeRef, isOver } = useDroppable({
        id,
        data: {
            acceptsType,
        }
    });

    return (
        <div
            ref={setNodeRef}
            className={`drop-zone ${isOver ? "drop-zone-over" : ""} ${piece ? "drop-zone-filled" : ""}`}
        >
            <div className="drop-zone-label">{label}</div>
            {piece ? (
                <DroppedPiece id={piece.id} label={piece.label} type={piece.type} zoneId={id} />
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
                <DropZone id="searchSpace" label="Search Space" acceptsType="searchSpace" piece={config?.searchSpace} />
                <DropZone id="problem" label="Problem" acceptsType="problem" piece={config?.problem}/>
                <DropZone id="algorithm" label="Algorithm" acceptsType="algorithm" piece={config?.algorithm}/>
                <DropZone id="mutation" label="Mutation" acceptsType="mutation" piece={config?.mutation}/>
                <DropZone id="acceptance" label="Acceptance Rule" acceptsType="acceptance" piece={config?.acceptance}/>
                <DropZone id="stopCondition" label="Stop Condition" acceptsType="stopCondition" piece={config?.stopCondition} />
                <DropZone id="observer" label="Observer" acceptsType="observer" piece={config?.observer}/>
            </div>
        </div>
    );
}