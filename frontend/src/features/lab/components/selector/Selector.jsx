
// Selector is responsible for displaying selectable puzzle pieces in tabs,
// determining whether each piece should be enabled or disabled,
// and enforcing runtime study restrictions.

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
// Types for which only one piece can be selected at a time in the selector
const SINGLE_SELECT_TYPES = new Set([
  "searchSpace",
  "populationModel",
  "generator",
  "selection",
  "parentSelectionRule",
  "crossover",
]);
// In runtime study mode, we further restrict the single-select types to ensure valid experiment configurations
const RUNTIME_STUDY_SINGLE_SELECT_TYPES = new Set([
  ...SINGLE_SELECT_TYPES,
  "problem",
  "stopCondition",
]);

// Use a fixed number of columns for the tab buttons in each row.
const TAB_COLUMNS = 5;

// This function checks if there are specific restrictions for runtime study mode based on the active tab and item being rendered.
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

  // Build a list of all valid tab keys and make sure the active tab  is still one of them, as it comes from sessionstorage.
  const validTabKeys = COMPONENT_TYPES.map((type) => type.key);
  const currentActiveTab = validTabKeys.includes(activeTab)  ? activeTab : validTabKeys[0] ?? "searchSpace";

  // If the active tab from session storage is not valid anymore , reset it to to the first valid tab.
  if (currentActiveTab !== activeTab) {
    setActiveTab(currentActiveTab);
  }

  const activeType = COMPONENT_TYPES.find((type) => type.key === currentActiveTab);
  const items = activeType ? catalog?.[activeType.catalogKey] ?? [] : [];

  // Calculate how many empty placeholder cells are needed to fill the last row of tabs so that the layout remains consistent.
  const fillerCount = (TAB_COLUMNS - (COMPONENT_TYPES.length % TAB_COLUMNS)) % TAB_COLUMNS;
  // Helper function to get the count of selected pieces for a given type, used for displaying the badge count on each tab.
  const getCount = (key) => {
    const group = puzzleConfig?.[key];
    return Array.isArray(group) ? group.length : 0;
  };

  const isSingleSelectType = (type) => isRuntimeStudy ? RUNTIME_STUDY_SINGLE_SELECT_TYPES.has(type) : SINGLE_SELECT_TYPES.has(type);

  const getSelectedSearchSpaceId = () => {
    const searchSpaces = puzzleConfig?.searchSpace;
    if (!Array.isArray(searchSpaces) || searchSpaces.length === 0) {
      return null;
    }
    return searchSpaces[0].id;
  };
  // This function checks if a given item is compatible with the currently selected search space.
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
    Object.values(puzzleConfig ?? {}).some( (group) => Array.isArray(group) && group.some((piece) => piece.id === itemId) );

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
     return activeType?.label?.toLowerCase() ?? currentActiveTab;
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