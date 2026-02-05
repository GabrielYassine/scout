import "../Selector.css";
import PuzzlePiece from "../../puzzlePiece/PuzzlePiece";

export default function RepresentationSelector({ catalog, catalogLoading, catalogError }) {
    const searchSpaces = catalog?.searchSpaces ?? [];

    if (catalogLoading) {
        return (
            <div className="selector-container">
                <div className="selector-title">Select Search Space</div>
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
                <div className="selector-title">Select Search Space</div>
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
            <div className="selector-title">Select Search Space</div>
            <div className="option-container">
                <div className="option-list">
                    {searchSpaces.map((searchSpace) => (
                        <PuzzlePiece key={searchSpace.id} id={searchSpace.id} label={searchSpace.name}/>
                    ))}
                </div>
                <div className="option-description"></div>
            </div>
        </div>
    );
}
