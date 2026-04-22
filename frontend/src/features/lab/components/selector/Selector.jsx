import "./Selector.css";
import PuzzlePiece from "../PuzzlePiece";
import { useSessionStorageState } from "@/shared/hooks/useSessionStorageState.js";

const COMPONENT_TYPES = [
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

const SINGLE_SELECT_TYPES = new Set([
  "searchSpace",
  "populationModel",
  "generator",
  "selection",
  "parentSelectionRule",
  "crossover",
]);

const RUNTIME_STUDY_SINGLE_SELECT_TYPES = new Set([
  ...SINGLE_SELECT_TYPES,
  "problem",
  "stopCondition",
]);

const TAB_COLUMNS = 5;

function getRuntimeStudyRestriction(tabKey, itemId) {
  if (tabKey === "observer") {
    return "Observers cannot be selected in runtime study mode";
  }

  if (tabKey === "searchSpace" && itemId !== "bitstring") {
    return "Runtime study currently supports only the BitString search space";
  }

  if (tabKey === "problem" && (itemId === "tsp" || itemId === "vrp")) {
    return "Runtime study currently supports only theoretical size-based problems";
  }

  if (tabKey === "stopCondition" && itemId !== "optimum-reached") {
    return "Runtime study requires the 'optimum-reached' stop condition";
  }

  return null;
}

export default function Selector({
  catalog,
  onPieceHover,
  onPieceLeave,
  puzzleConfig,
  params,
}) {
  const [activeTab, setActiveTab] = useSessionStorageState(
    "scout:activeSelector",
    "searchSpace"
  );

  const runMode = params?.global?.experimentType ?? "run";
  const isRuntimeStudy = runMode === "runtimeStudy";

  const validTabKeys = COMPONENT_TYPES.map((type) => type.key);
  const currentActiveTab = validTabKeys.includes(activeTab)
    ? activeTab
    : validTabKeys[0] ?? "searchSpace";

  if (currentActiveTab !== activeTab) {
    setActiveTab(currentActiveTab);
  }

  const activeType = COMPONENT_TYPES.find((type) => type.key === currentActiveTab);
  const items = activeType ? catalog?.[activeType.catalogKey] ?? [] : [];
  const fillerCount = (TAB_COLUMNS - (COMPONENT_TYPES.length % TAB_COLUMNS)) % TAB_COLUMNS;

  const getCount = (key) => {
    const group = puzzleConfig?.[key];
    return Array.isArray(group) ? group.length : 0;
  };

  const isSingleSelectType = (type) =>
    isRuntimeStudy
      ? RUNTIME_STUDY_SINGLE_SELECT_TYPES.has(type)
      : SINGLE_SELECT_TYPES.has(type);

  const getSelectedSearchSpaceId = () => {
    const searchSpaces = puzzleConfig?.searchSpace;
    if (!Array.isArray(searchSpaces) || searchSpaces.length === 0) {
      return null;
    }
    return searchSpaces[0].id;
  };

  const isItemCompatibleWithSearchSpace = (item) => {
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

  const isIdAlreadyInConfig = (itemId) =>
    Object.values(puzzleConfig ?? {}).some(
      (group) => Array.isArray(group) && group.some((piece) => piece.id === itemId)
    );

  const isDisabledBySingleSelectRule = (item) => {
    if (!isSingleSelectType(currentActiveTab)) {
      return false;
    }

    const currentGroup = puzzleConfig?.[currentActiveTab];
    if (!Array.isArray(currentGroup) || currentGroup.length === 0) {
      return false;
    }

    const alreadySelectedThisType = currentGroup.some((piece) => piece.id === item.id);
    return !alreadySelectedThisType;
  };

  const getSingleSelectLabel = () => {
    const type = COMPONENT_TYPES.find((entry) => entry.key === currentActiveTab);
    return type?.label?.toLowerCase() ?? currentActiveTab;
  };

  const getDisableReason = (item) => {
    if (isRuntimeStudy) {
      const runtimeStudyRestriction = getRuntimeStudyRestriction(currentActiveTab, item.id);
      if (runtimeStudyRestriction) {
        return runtimeStudyRestriction;
      }
    }

    if (!isItemCompatibleWithSearchSpace(item)) {
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
        {COMPONENT_TYPES.map(({ key, label }) => {
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

        {Array.from({ length: fillerCount }).map((_, index) => (
          <div
            key={`tab-filler-${index}`}
            className="tab-button tab-button--placeholder"
            aria-hidden="true"
          />
        ))}
      </div>

      <div className="option-list-outer">
        <div className="option-list">
          {items.map((item) => {
            const disabledReason = getDisableReason(item);

            return (
              <PuzzlePiece
                key={item.id}
                id={item.id}
                label={item.displayName}
                type={currentActiveTab}
                onHover={onPieceHover}
                onLeave={onPieceLeave}
                isDisabled={disabledReason !== null}
                disabledReason={disabledReason}
              />
            );
          })}
        </div>
      </div>
    </div>
  );
}