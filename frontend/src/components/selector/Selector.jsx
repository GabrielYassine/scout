import "./Selector.css";
import PuzzlePiece from "./PuzzlePiece";
import { useSessionStorageState } from "../../hooks/useSessionStorageState.js";

const componentTypesAll = [
    { key: "searchSpace", label: "Search Space", catalogKey: "searchSpaces" },
    { key: "problem", label: "Problem", catalogKey: "problems" },
    { key: "generator", label: "Generator", catalogKey: "generators" },
    { key: "selection", label: "Selection Rule", catalogKey: "selectionRules" },
    { key: "populationModel", label: "Population Model", catalogKey: "populationModels" },
    { key: "parentSelectionRule", label: "Parent Selection", catalogKey: "parentSelectionRules" },
    { key: "crossover", label: "Crossover", catalogKey: "crossovers" },
    { key: "stopCondition", label: "Stop Condition", catalogKey: "stopConditions" },
    { key: "observer", label: "Observer", catalogKey: "observers" },
];

const singleSelectTypes = new Set([
    "searchSpace",
    "populationModel",
    "generator",
    "selection",
    "parentSelectionRule",
    "crossover",
]);
const runtimeStudySingleSelectTypes = new Set([
    ...singleSelectTypes,
    "problem",
    "stopCondition",
]);


export default function Selector({
    catalog,
    onPieceHover,
    onPieceLeave,
    puzzleConfig,
    params,
}) {
    const [activeTab, setActiveTab] = useSessionStorageState("scout:activeSelector", "searchSpace");
    const runMode = params?.global?.experimentType ?? "run";
    const isRuntimeStudy = runMode === "runtimeStudy";

    const validTabKeys = componentTypesAll.map((t) => t.key);
    const currentActiveTab = validTabKeys.includes(activeTab)
        ? activeTab
        : (validTabKeys[0] || "searchSpace");

    if (currentActiveTab !== activeTab) {
        setActiveTab(currentActiveTab);
    }

    const activeType = componentTypesAll.find((type) => type.key === currentActiveTab);
    const items = activeType ? (catalog?.[activeType.catalogKey] ?? []) : [];

    const getCount = (key) => {
        if (!puzzleConfig || !puzzleConfig[key]) return 0;
        return Array.isArray(puzzleConfig[key]) ? puzzleConfig[key].length : 0;
    };

    const isSingleSelectType = (type) => {
        if (isRuntimeStudy) {
            return runtimeStudySingleSelectTypes.has(type);
        }
        return singleSelectTypes.has(type);
    };

    const getSelectedSearchSpaceId = () => {
        if (!puzzleConfig || !Array.isArray(puzzleConfig.searchSpace) || puzzleConfig.searchSpace.length === 0) {
            return null;
        }
        return puzzleConfig.searchSpace[0].id;
    };

    const isItemCompatible = (item) => {
        if (isRuntimeStudy) {
                if (currentActiveTab === "observer") {
                    return false;
                }

                if (currentActiveTab === "searchSpace") {
                    return item.id === "bitstring";
                }

                if (currentActiveTab === "problem") {
                    if (item.id === "tsp" || item.id === "vrp") {
                        return false;
                    }
                }

                if (currentActiveTab === "stopCondition") {
                    return item.id === "optimum-reached";
                }
         }
        if (currentActiveTab === "searchSpace") {
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

    const isIdAlreadyInConfig = (itemId) => {
        if (!puzzleConfig) return false;

        return Object.values(puzzleConfig).some(
            (group) => Array.isArray(group) && group.some((piece) => piece.id === itemId)
        );
    };

    const isDisabledBySingleSelectRule = (item) => {
        if (!isSingleSelectType(currentActiveTab)) {
            return false;
        }

        const currentGroup = puzzleConfig?.[currentActiveTab];
        if (!Array.isArray(currentGroup) || currentGroup.length === 0) {
            return false;
        }

        const alreadySelectedThisType = currentGroup.some((piece) => piece.id === item.id);
        if (alreadySelectedThisType) {
            return false;
        }

        return true;
    };

    const getSingleSelectLabel = () => {
        const type = componentTypesAll.find((t) => t.key === currentActiveTab);
        return type?.label?.toLowerCase() ?? currentActiveTab;
    };

    const getDisableReason = (item) => {
        if (isRuntimeStudy) {
                if (currentActiveTab === "observer") {
                    return "Observers cannot be selected in runtime study mode";
                }

                if (currentActiveTab === "searchSpace" && item.id !== "bitstring") {
                    return "Runtime study currently supports only the BitString search space";
                }

                if (
                    currentActiveTab === "problem" &&
                    (item.id === "tsp" || item.id === "vrp")
                ) {
                    return "Runtime study currently supports only theoretical size-based problems";
                }

                if (
                    currentActiveTab === "stopCondition" &&
                    item.id !== "optimum-reached"
                ) {
                    return "Runtime study requires the 'optimum-reached' stop condition";
                }
       }

        if (!isItemCompatible(item)) {
            return "Not compatible with the selected search space";
        }

        if (isIdAlreadyInConfig(item.id)) {
            return "This item is already added to the configuration";
        }

        if (isDisabledBySingleSelectRule(item)) {
            return `Only one ${getSingleSelectLabel()} can be selected`;
        }

        return null;
    };

    return (
        <div className="selector-container">
            <div className="tab-buttons-row">
                {componentTypesAll.map(({ key, label }) => {
                    const count = getCount(key);

                    return (
                        <button
                            key={key}
                            className={`tab-button ${currentActiveTab === key ? "active" : ""}`}
                            onClick={() => setActiveTab(key)}
                        >
                            <span className={`count-badge count-badge-${key}`}>{count}</span>
                            {label}
                        </button>
                    );
                })}
            </div>

            <div className="option-list-outer">
                <div className="option-list">
                    {items.map((item) => {
                        const disabledReason = getDisableReason(item);
                        const isDisabled = disabledReason !== null;

                        return (
                            <PuzzlePiece
                                key={item.id}
                                id={item.id}
                                label={item.displayName}
                                type={currentActiveTab}
                                onHover={onPieceHover}
                                onLeave={onPieceLeave}
                                isDisabled={isDisabled}
                                disabledReason={disabledReason}
                            />
                        );
                    })}
                </div>
            </div>
        </div>
    );
}