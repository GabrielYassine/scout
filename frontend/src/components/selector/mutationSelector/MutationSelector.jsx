import "../Selector.css";
import PuzzlePiece from "../../puzzlePiece/PuzzlePiece";

export default function MutationSelector({ catalog, catalogLoading}) {
    const mutations = catalog?.mutations ?? [];
    return (
        <div className="selector-container">
            <div className="selector-title">Select Mutation</div>
            <div className="option-container">
                <div className="option-list">
                    {mutations.map((mutation) => (
                        <PuzzlePiece
                            key={mutation.id}
                            id={mutation.id}
                            label={mutation.name}
                            type="mutation"
                        />
                    ))}
                </div>
                <div className="option-description"></div>
            </div>
        </div>
    );
}