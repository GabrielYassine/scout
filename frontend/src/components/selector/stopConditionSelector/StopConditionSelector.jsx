import "../Selector.css";
import PuzzlePiece from "../../puzzlePiece/PuzzlePiece";

export default function StopConditionSelector({ catalog, catalogLoading }) {
    const stopConditions = catalog?.stopConditions ?? [];
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
                            type="stopCondition"
                        />
                    ))}
                </div>
                <div className="option-description"></div>
            </div>
        </div>
    );
}