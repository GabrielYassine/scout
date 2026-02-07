import "../Selector.css";
import PuzzlePiece from "../../puzzlePiece/PuzzlePiece";

export default function ObserverSelector({ catalog, catalogLoading, catalogError }) {
    const observers = catalog?.observers ?? [];

    if (catalogLoading) {
        return (
            <div className="selector-container">
                <div className="selector-title">Select Observer</div>
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
                <div className="selector-title">Select Observer</div>
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
