// Shared selector configuration and rule helpers.
export const COMPONENT_TYPES = [
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

export const SINGLE_SELECT_TYPES = new Set([
  "searchSpace",
  "populationModel",
  "generator",
  "selection",
  "parentSelectionRule",
  "crossover",
]);

export const RUNTIME_STUDY_SINGLE_SELECT_TYPES = new Set([
  ...SINGLE_SELECT_TYPES,
  "problem",
  "stopCondition",
]);

export const TAB_COLUMNS = 5;

// Return a runtime-study-specific restriction message for a given tab/item pair.
export function getRuntimeStudyRestriction(tabKey, itemId) {
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

// Return whether a selector type is single-select in the current mode.
export function isSingleSelectType(type, isRuntimeStudy) {
  return isRuntimeStudy
    ? RUNTIME_STUDY_SINGLE_SELECT_TYPES.has(type)
    : SINGLE_SELECT_TYPES.has(type);
}

// Return the selected search space id from puzzleConfig, if any.
export function getSelectedSearchSpaceId(puzzleConfig) {
  const searchSpaces = puzzleConfig?.searchSpace;
  if (!Array.isArray(searchSpaces) || searchSpaces.length === 0) {
    return null;
  }

  return searchSpaces[0].id;
}

// Check whether an item is compatible with the currently selected search space.
export function isItemCompatibleWithSearchSpace({
  item,
  currentActiveTab,
  puzzleConfig,
}) {
  if (currentActiveTab === "searchSpace") {
    return true;
  }

  const selectedSearchSpaceId = getSelectedSearchSpaceId(puzzleConfig);
  if (!selectedSearchSpaceId) {
    return true;
  }

  if (!item.supportedSearchSpaces || item.supportedSearchSpaces.length === 0) {
    return true;
  }

  return item.supportedSearchSpaces.includes(selectedSearchSpaceId);
}

// Check whether an item id already exists anywhere in the current configuration.
export function isIdAlreadyInConfig(itemId, puzzleConfig) {
  return Object.values(puzzleConfig ?? {}).some(
    (group) => Array.isArray(group) && group.some((piece) => piece.id === itemId)
  );
}

// Check whether the current item should be disabled by a single-select rule.
export function isDisabledBySingleSelectRule({
  item,
  currentActiveTab,
  puzzleConfig,
  isRuntimeStudy,
}) {
  if (!isSingleSelectType(currentActiveTab, isRuntimeStudy)) {
    return false;
  }

  const currentGroup = puzzleConfig?.[currentActiveTab];
  if (!Array.isArray(currentGroup) || currentGroup.length === 0) {
    return false;
  }

  const alreadySelectedThisType = currentGroup.some((piece) => piece.id === item.id);
  return !alreadySelectedThisType;
}

// Return the disable reason for a selector item, or null if it should stay enabled.
export function getDisableReason({ item,  currentActiveTab, activeType,  puzzleConfig,  isRuntimeStudy, }) {
  if (isRuntimeStudy) {
    const runtimeStudyRestriction = getRuntimeStudyRestriction(currentActiveTab, item.id);
    if (runtimeStudyRestriction) {
      return runtimeStudyRestriction;
    }
  }

  if (
    !isItemCompatibleWithSearchSpace({ item, currentActiveTab,  puzzleConfig,  })
  ) {
    return "Not compatible with the selected search space";
  }

  if (isIdAlreadyInConfig(item.id, puzzleConfig)) {
    return "This item is already added to the configuration";
  }

  if (
    isDisabledBySingleSelectRule({ item, currentActiveTab,  puzzleConfig,  isRuntimeStudy, })
  ) {
    const label = activeType?.label?.toLowerCase() ?? currentActiveTab;
    return `Only one ${label} can be selected`;
  }

  return null;
}