import "../Selector.css";
import PuzzlePiece from "../../puzzlePiece/PuzzlePiece";

export default function PopulationSelector({ catalog, catalogLoading, catalogError }) {
    const acceptanceRules = catalog?.acceptanceRules ?? [];

    if (catalogLoading) {
        return (
            <div className="selector-container">
                <div className="selector-title">Select Acceptance Rule</div>
                <div className="option-container">
                    <div className="option-list">
                        <div>Loading...</div>
                    </div>
                    <div className="option-description"></div>
                </div>
            </div>
        );
    }

    if (catalogError) {
        return (
            <div className="selector-container">
                <div className="selector-title">Select Acceptance Rule</div>
                <div className="option-container">
                    <div className="option-list">
                        <div>Error loading catalog: {catalogError}</div>
                    </div>
                    <div className="option-description"></div>
                </div>
            </div>
        );
    }

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
                        />
                    ))}
                </div>
                <div className="option-description"></div>
            </div>
        </div>
    );
}