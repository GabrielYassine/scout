import "../Selector.css";
import PuzzlePiece from "../../puzzlePiece/PuzzlePiece";

export default function RepresentationSelector({ catalog, catalogLoading }) {
    const searchSpaces = catalog?.searchSpaces ?? [];
    return (
        <div className="selector-container">
            <div className="selector-title">Select Search Space</div>
            <div className="option-container">
                <div className="option-list">
                    {searchSpaces.map((searchSpace) => (
                        <PuzzlePiece
                            key={searchSpace.id}
                            id={searchSpace.id}
                            label={searchSpace.name}
                            type="searchSpace"
                        />
                    ))}
                </div>
                <div className="option-description"></div>
            </div>
        </div>
    );
}
