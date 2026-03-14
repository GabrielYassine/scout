import "./Selector.css";
import AlgorithmTypeSelector from "./AlgorithmTypeSelector.jsx";
import PuzzlePiece from "../puzzlePiece/PuzzlePiece";
import { useSessionStorageState } from "../../hooks/useSessionStorageState.js";
import { usePuzzleConfig } from "../../contexts/PuzzleConfigContext.jsx";

const componentTypesAll = [
    { key: "searchSpace", label: "Search Space", catalogKey: "searchSpaces" },
    { key: "problem", label: "Problem", catalogKey: "problems" },
    { key: "heuristicFunction", label: "Heuristic", catalogKey: "heuristicFunctions" },
    { key: "constructionPolicy", label: "Construction", catalogKey: "constructionPolicies" },
    { key: "pheromoneModel", label: "Pheromone", catalogKey: "pheromoneModels" },
    { key: "mutation", label: "Mutation", catalogKey: "mutations" },
    { key: "acceptance", label: "Acceptance Rule", catalogKey: "acceptanceRules" },
    { key: "populationModel", label: "Population Model", catalogKey: "populationModels" },
    { key: "stopCondition", label: "Stop Condition", catalogKey: "stopConditions" },
    { key: "observer", label: "Observer", catalogKey: "observers" },
];

export default function Selector({ catalog, catalogLoading, catalogError ,onPieceHover, onPieceLeave, puzzleConfig}) {
    const { algorithmType, setAlgorithmType } = usePuzzleConfig();
    const [activeTab, setActiveTab] = useSessionStorageState("scout:activeSelector", "searchSpace");

    const getComponentTypesForAlgorithm = () => {
        if (!algorithmType || !catalog?.algorithmTypes) return [];

        const selectedAlgo = catalog.algorithmTypes.find(algo => algo.id === algorithmType);
        if (!selectedAlgo || !selectedAlgo.componentTypes) return [];

        return componentTypesAll.filter(type => selectedAlgo.componentTypes.includes(type.key));
    };

    const componentTypes = getComponentTypesForAlgorithm();

    const validTabKeys = componentTypes.map(t => t.key);
    const currentActiveTab = validTabKeys.includes(activeTab) ? activeTab : (validTabKeys[0] || "searchSpace");
    if (currentActiveTab !== activeTab) {
        setActiveTab(currentActiveTab);
    }

    const activeType = componentTypes.find(type => type.key === currentActiveTab);
    const items = activeType ? (catalog?.[activeType.catalogKey] ?? []) : [];
    const getCount = (key) => {
        if (!puzzleConfig || !puzzleConfig[key]) return 0;
        return Array.isArray(puzzleConfig[key]) ? puzzleConfig[key].length : 0;
    };

    const getSelectedSearchSpaceId = () => {
        if (!puzzleConfig || !puzzleConfig.searchSpace || puzzleConfig.searchSpace.length === 0) {
            return null;
        }
        return puzzleConfig.searchSpace[0].id;
    };

    const isItemCompatible = (item) => {
        if (currentActiveTab === 'searchSpace') {
            return true;
        }
        const selectedSearchSpaceId = getSelectedSearchSpaceId();
        if (!selectedSearchSpaceId) {
            return true;
        }
        if (!item.supportedSearchSpaces || item.supportedSearchSpaces.length === 0) {
            return true;
        }
        return item.supportedSearchSpaces.includes(selectedSearchSpaceId);
    };

    const handleSelectAlgorithm = (algoType) => {
        setAlgorithmType(algoType);
        if (catalog?.algorithmTypes) {
            const selectedAlgo = catalog.algorithmTypes.find(algo => algo.id === algoType);
            if (selectedAlgo?.componentTypes && selectedAlgo.componentTypes.length > 0) {
                const firstComponentKey = selectedAlgo.componentTypes[0];
                setActiveTab(firstComponentKey);
            }
        }
    };

    if (!algorithmType) {
        return (
            <AlgorithmTypeSelector
                algorithmTypes={catalog?.algorithmTypes || []}
                onSelectAlgorithm={handleSelectAlgorithm}
            />
        );
    }

    return (
        <div className="selector-container">
            <div className="tab-buttons-row">
                {componentTypes.map(({ key, label }) => {
                    const count = getCount(key);
                    return (
                        <button key={key} className={`tab-button ${currentActiveTab === key ? "active" : ""}`} onClick={() => setActiveTab(key)}>
                            <span className={`count-badge count-badge-${key}`}>{count}</span>
                            {label}
                        </button>
                    );
                })}
            </div>
            <div className="option-list">
                {catalogLoading && <div style={{ padding: '20px', textAlign: 'center' }}>Loading catalog...</div>}
                {catalogError && <div style={{ padding: '20px', color: 'red', textAlign: 'center' }}>Error: {catalogError}</div>}
                {!catalogLoading && !catalogError && items.length === 0 && (
                    <div style={{ padding: '20px', textAlign: 'center', color: '#666' }}>No items available</div>
                )}
                {!catalogLoading && !catalogError && items.map((item) => (
                    <PuzzlePiece
                        key={item.id}
                        id={item.id}
                        label={item.displayName}
                        type={currentActiveTab}
                        onHover={onPieceHover}
                        onLeave={onPieceLeave}
                        isDisabled={!isItemCompatible(item)}
                    />
                ))}
            </div>
        </div>
    );
}