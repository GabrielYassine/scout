import "../Selector.css";
import PuzzlePiece from "../../puzzlePiece/PuzzlePiece";

export default function PopulationSelector({ catalog, catalogLoading }) {
    return (
        <div className="selector-container">
            <div className="selector-title"></div>
            <div className="option-container">
                <div className="option-list">
                </div>
                <div className="option-description"></div>
            </div>
        </div>
    );
}