import "../Selector.css";
import PuzzlePiece from "../../puzzlePiece/PuzzlePiece";

export default function MutationSelector({ catalog, catalogLoading, catalogError }) {
    const mutations = catalog?.mutations ?? [];

    if (catalogLoading) {
        return (
            <div className="selector-container">
                <div className="selector-title">Select Mutation</div>
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
                <div className="selector-title">Select Mutation</div>
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
            <div className="selector-title">Select Mutation</div>
            <div className="option-container">
                <div className="option-list">
                    {mutations.map((mutation) => (
                        <PuzzlePiece
                            key={mutation.id}
                            id={mutation.id}
                            label={mutation.name}
                        />
                    ))}
                </div>
                <div className="option-description"></div>
            </div>
        </div>
    );
}