import "../Selector.css";
import PuzzlePiece from "../../puzzlePiece/PuzzlePiece";

export default function ObserverSelector({ catalog }) {
    const observers = catalog?.observers ?? [];
    return (
        <div className="selector-container">
            <div className="selector-title">Select Observer</div>
            <div className="option-container">
                <div className="option-list">
                    {observers.map((observer) => (
                        <PuzzlePiece
                            key={observer.id}
                            id={observer.id}
                            label={observer.name}
                            type="observer"
                        />
                    ))}
                </div>
                <div className="option-description"></div>
            </div>
        </div>
    );
}
