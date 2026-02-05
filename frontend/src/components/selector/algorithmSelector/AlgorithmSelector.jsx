import "../Selector.css";
import PuzzlePiece from "../../puzzlePiece/PuzzlePiece";

export default function AlgorithmSelector({ catalog, catalogLoading}) {
    const algorithms = catalog?.algorithms ?? [];
    return (
        <div className="selector-container">
            <div className="selector-title">Select Algorithm</div>
            <div className="option-container">
                <div className="option-list">
                    {algorithms.map((algorithm) => (
                        <PuzzlePiece
                            key={algorithm.id}
                            id={algorithm.id}
                            label={algorithm.name}
                            type="algorithm"
                        />
                    ))}
                </div>
                <div className="option-description"></div>
            </div>
        </div>
    );
}