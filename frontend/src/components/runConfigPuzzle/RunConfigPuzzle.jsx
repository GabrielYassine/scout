import { useDroppable } from "@dnd-kit/core";
import "./RunConfigPuzzle.css";
import TypeColumn from "./TypeColumn.jsx";

export default function RunConfigPuzzle({ config, onRemovePiece }) {
    const { setNodeRef, isOver } = useDroppable({
        id: "shared-drop-area",
        data: {
            acceptsAll: true,
        }
    });

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

    return (
        <div ref={setNodeRef} className={`shared-drop-area ${isOver ? "drop-area-over" : ""}`}>
            {componentTypes.map(({ key, label }) => (
                <TypeColumn
                    key={key}
                    type={key}
                    label={label}
                    pieces={Array.isArray(config?.[key]) ? config[key] : []}
                    onRemovePiece={onRemovePiece}
                />
            ))}
        </div>
    );
}