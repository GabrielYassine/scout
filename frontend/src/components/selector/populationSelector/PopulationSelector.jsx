import "../Selector.css";
import PuzzlePiece from "../../puzzlePiece/PuzzlePiece";

export default function PopulationSelector({ catalog, catalogLoading }) {
    const acceptanceRules = catalog?.acceptanceRules ?? [];
    return (
        <div className="selector-container">
            <div className="selector-title">Select Acceptance Rule</div>
            <div className="option-container">
                <div className="option-list">
                    {acceptanceRules.map((rule) => (
                        <PuzzlePiece
                            key={rule.id}
                            id={rule.id}
                            label={rule.name}
                            type="acceptance"
                        />
                    ))}
                </div>
                <div className="option-description"></div>
            </div>
        </div>
    );
}