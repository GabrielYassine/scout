import { useDroppable } from "@dnd-kit/core";
import { useMemo } from "react";
import "./RunConfigPuzzle.css";
import TypeColumn from "./TypeColumn.jsx";

const componentTypes = [
    { key: "searchSpace", label: "Search Space" },
    { key: "problem", label: "Problem" },
    { key: "algorithm", label: "Algorithm" },
    { key: "mutation", label: "Mutation" },
    { key: "acceptance", label: "Acceptance Rule" },
    { key: "populationModel", label: "Population Model" },
    { key: "stopCondition", label: "Stop Condition" },
    { key: "observer", label: "Observer" },
];

export default function RunConfigPuzzle({config, onRemovePiece, onPieceHover, onPieceLeave,}) {
    const { setNodeRef, isOver } = useDroppable({
        id: "shared-drop-area",
        data: { acceptsAll: true },
    });

    const enrichedConfig = useMemo(() => {
        const result = {};
        componentTypes.forEach(({ key: typeKey }) => {
            result[typeKey] = Array.isArray(config?.[typeKey]) ? config[typeKey] : [];
        });
        return result;
    }, [config]);

    return (
        <div ref={setNodeRef} className={`shared-drop-area ${isOver ? "drop-area-over" : ""}`}>
            {componentTypes.map(({ key, label }, index) => (
                <TypeColumn
                    key={key}
                    type={key}
                    label={label}
                    index={index}
                    pieces={enrichedConfig[key] || []}
                    totalCols={componentTypes.length}
                    onRemovePiece={onRemovePiece}
                    onPieceHover={onPieceHover}
                    onPieceLeave={onPieceLeave}
                />
            ))}
        </div>
    );
}