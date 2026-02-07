import "../Selector.css";
import PuzzlePiece from "../../puzzlePiece/PuzzlePiece";

export default function PopulationSelector({ catalog, catalogLoading }) {
    const models = catalog?.populationModels ?? [];
    return (
        <div className="selector-container">
            <div className="selector-title">Select Population Model</div>
            <div className="option-container">
                <div className="option-list">
                  {models.map((m) => (
                    <PuzzlePiece
                      key={m.id}
                      id={m.id}
                      label={m.name}
                      type="populationModel"
                    />
                  ))}
                </div>
                <div className="option-description"></div>
            </div>
        </div>
    );
}