import "./AlgorithmTypeSelector.css";

export default function AlgorithmTypeSelector({ algorithmTypes, onSelectAlgorithm }) {
    if (!algorithmTypes || algorithmTypes.length === 0) {
        return (
            <div className="algorithm-type-selector">
                <div className="algorithm-selection-prompt">Loading algorithm types...</div>
            </div>
        );
    }

    return (
        <div className="algorithm-type-selector">
            <div className="algorithm-selection-prompt">
                <h2>First, pick an Algorithm Type</h2>
                <p>Choose the type of algorithm you want to use</p>
            </div>
            <div className="algorithm-type-buttons">
                {algorithmTypes.map((algo) => (
                    <button
                        key={algo.id}
                        className="algorithm-type-button"
                        onClick={() => onSelectAlgorithm(algo.id)}
                        title={algo.description}
                    >
                        <div className="algorithm-type-name">{algo.displayName}</div>
                        <div className="algorithm-type-description">{algo.description}</div>
                    </button>
                ))}
            </div>
        </div>
    );
}
