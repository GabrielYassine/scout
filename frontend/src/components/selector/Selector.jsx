import "./Selector.css";
import PuzzlePiece from "../puzzlePiece/PuzzlePiece";
import { useSessionStorageState } from "../../hooks/useSessionStorageState.js";

const componentTypes = [
    { key: "searchSpace", label: "Search Space", catalogKey: "searchSpaces" },
    { key: "problem", label: "Problem", catalogKey: "problems" },
    { key: "algorithm", label: "Algorithm", catalogKey: "algorithms" },
    { key: "mutation", label: "Mutation", catalogKey: "mutations" },
    { key: "acceptance", label: "Acceptance Rule", catalogKey: "acceptanceRules" },
    { key: "populationModel", label: "Population Model", catalogKey: "populationModels" },
    { key: "stopCondition", label: "Stop Condition", catalogKey: "stopConditions" },
    { key: "observer", label: "Observer", catalogKey: "observers" },
];

export default function Selector({ catalog, catalogLoading, catalogError ,onPieceHover, onPieceLeave, puzzleConfig}) {
    const [activeTab, setActiveTab] = useSessionStorageState("scout:activeSelector", "searchSpace");
    const activeType = componentTypes.find(type => type.key === activeTab);
    const items = catalog?.[activeType.catalogKey] ?? [];

    // Function to get count of pieces for a given type
    const getCount = (key) => {
        if (!puzzleConfig || !puzzleConfig[key]) return 0;
        return Array.isArray(puzzleConfig[key]) ? puzzleConfig[key].length : 0;
    };

    return (
        <div className="selector-container">
            <div className="tab-buttons-row">
                {componentTypes.map(({ key, label }) => {
                    const count = getCount(key);
                    return (
                        <button key={key} className={`tab-button ${activeTab === key ? "active" : ""}`} onClick={() => setActiveTab(key)}>
                            <span className={`count-badge count-badge-${key}`}>{count}</span>
                            {label}
                        </button>
                    );
                })}
            </div>
            <div className="option-list">
                {items.map((item) => (
                    <PuzzlePiece
                        key={item.id}
                        id={item.id}
                        label={item.name}
                        type={activeTab}
                        onHover={onPieceHover}
                        onLeave={onPieceLeave}
                    />
                ))}
            </div>
        </div>
    );
}