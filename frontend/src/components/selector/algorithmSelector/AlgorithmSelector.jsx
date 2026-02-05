import "../Selector.css";
import PuzzlePiece from "../../puzzlePiece/PuzzlePiece";

export default function AlgorithmSelector({ catalog, catalogLoading, catalogError }) {
    const algorithms = catalog?.algorithms ?? [];

    if (catalogLoading) {
        return (
            <div className="selector-container">
                <div className="selector-title">Select Algorithm</div>
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
                <div className="selector-title">Select Algorithm</div>
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
            <div className="selector-title">Select Algorithm</div>
            <div className="option-container">
                <div className="option-list">
                    {algorithms.map((algorithm) => (
                        <PuzzlePiece
                            key={algorithm.id}
                            id={algorithm.id}
                            label={algorithm.name}
                        />
                    ))}
                </div>
                <div className="option-description"></div>
            </div>
        </div>
    );
}