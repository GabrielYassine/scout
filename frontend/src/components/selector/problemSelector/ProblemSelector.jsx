import "../Selector.css";
import PuzzlePiece from "../../puzzlePiece/PuzzlePiece";

export default function ProblemSelector({ catalog, catalogLoading, catalogError }) {
    const problems = catalog?.problems ?? [];

    if (catalogLoading) {
        return (
            <div className="selector-container">
                <div className="selector-title">Select Problem</div>
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
                <div className="selector-title">Select Problem</div>
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
            <div className="selector-title">Select Problem</div>
            <div className="option-container">
                <div className="option-list">
                    {problems.map((problem) => (
                        <PuzzlePiece
                            key={problem.id}
                            id={problem.id}
                            label={problem.name}
                        />
                    ))}
                </div>
                <div className="option-description"></div>
            </div>
        </div>
    );
}