import "../Selector.css";
import PuzzlePiece from "../../puzzlePiece/PuzzlePiece";

export default function StopConditionSelector({ catalog, catalogLoading, catalogError }) {
    const stopConditions = catalog?.stopConditions ?? [];

    if (catalogLoading) {
        return (
            <div className="selector-container">
                <div className="selector-title">Select Stop Condition</div>
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
                <div className="selector-title">Select Stop Condition</div>
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
            <div className="selector-title">Select Stop Condition</div>
            <div className="option-container">
                <div className="option-list">
                    {stopConditions.map((stopCondition) => (
                        <PuzzlePiece
                            key={stopCondition.id}
                            id={stopCondition.id}
                            label={stopCondition.name}
                        />
                    ))}
                </div>
                <div className="option-description"></div>
            </div>
        </div>
    );
}