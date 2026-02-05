import "../Selector.css";
import PuzzlePiece from "../../puzzlePiece/PuzzlePiece";

export default function ProblemSelector({ catalog, catalogLoading }) {
    const problems = catalog?.problems ?? [];
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
                            type="problem"
                        />
                    ))}
                </div>
                <div className="option-description"></div>
            </div>
        </div>
    );
}