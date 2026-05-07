/**
  *
  * @author s235257 & s230632
 */

import { generatePuzzleKey } from "@/shared/util/puzzleGenerator.js";

export const GRID_COLUMNS = 6;

export const componentTypes = [
  "searchSpace",
  "problem",
  "generator",
  "selection",
  "populationModel",
  "parentSelectionRule",
  "crossover",
  "stopCondition",
  "observer",
];
export const DEFAULT_CONFIG_ID = "config-1";
export const DEFAULT_CONFIG_NAME = "Config 1";

export function createFallbackConfig() {
  return createDefaultConfig(DEFAULT_CONFIG_ID, DEFAULT_CONFIG_NAME);
}

export function normalizeConfigList(configs, fallbackToDefault = true) {
  if (Array.isArray(configs)) {
    return configs.map(normalizeStoredConfig);
  }

  return fallbackToDefault ? [createFallbackConfig()] : [];
}

export function getNextConfigName(configs) {
  const safeConfigs = Array.isArray(configs) ? configs : [];

  const maxNumber = safeConfigs.reduce((max, config) => {
    const match = String(config?.name ?? "").match(/^Config\s+(\d+)$/i);
    if (!match) return max;

    const value = Number(match[1]);
    return Number.isFinite(value) ? Math.max(max, value) : max;
  }, 0);

  return `Config ${maxNumber + 1}`;
}

export const cloneTspInstance = (tsp) =>
  tsp ? { ...tsp,  cities: (tsp.cities ?? []).map((city) => ({ ...city })),  }  : null;

export const cloneVrpInstance = (vrp) =>
  vrp ? {  ...vrp,  depot: vrp.depot ? { ...vrp.depot } : null,  customers: (vrp.customers ?? []).map((customer) => ({ ...customer })),  }   : null;

export const createEmptyPuzzleConfig = () => ({
  searchSpace: [],
  problem: [],
  generator: [],
  selection: [],
  populationModel: [],
  parentSelectionRule: [],
  crossover: [],
  stopCondition: [],
  observer: [],
});

export const createEmptyParams = () => ({
  global: {
    experimentType: "run",
    seed: 1,
    runTimes: 1,
    logEveryIterations: 10,
    wsUpdateEveryIterations: 100,
    repetitionsPerSize: 10,
    problemSizes: "100, 200, 400, 800",
    wsUpdateEverySizes: 1,
  },
  searchSpace: {},
  problem: {},
  generator: {},
  selection: {},
  populationModel: {},
  parentSelectionRule: {},
  crossover: {},
  stopCondition: {},
  observer: {},
});

export const createDefaultConfig = (id, name) => ({
  id,
  name,
  placedPieces: [],
  params: createEmptyParams(),
  tspInstance: null,
  vrpInstance: null,
});

function clonePlacedPiece(piece) {
  return {
    id: piece.id,
    label: piece.label,
    type: piece.type,
    puzzleData: piece.puzzleData,
  };
}

function normalizeIds(value, single = false) {
  if (value == null) return [];
  const ids = Array.isArray(value) ? value : [value];
  return single ? ids.slice(0, 1) : ids;
}

function mapIdsToPieces(ids, catalogItems, pieceType, single = false) {
  return normalizeIds(ids, single)
    .map((id) => {
      const item = catalogItems.find((x) => x.id === id);
      return item ? { id: item.id, label: item.displayName, type: pieceType } : null;
    })
    .filter(Boolean);
}

function buildGlobalParamsFromRunRequest(runRequest) {
  return {
    experimentType: "run",
    seed: runRequest.seed || Date.now(),
    runTimes: runRequest.runTimes || 1,
    logEveryIterations: runRequest.logEveryIterations || 100,
    wsUpdateEveryIterations: runRequest.wsUpdateEveryIterations || 100,
    problemSizes: "100, 200, 400, 800",
    repetitionsPerSize: 10,
    wsUpdateEverySizes: 1,
  };
}
function buildGlobalParamsFromRuntimeStudyRequest(runtimeStudyRequest) {
  return {
    experimentType: "runtimeStudy",
    seed: runtimeStudyRequest.seed || Date.now(),
    runTimes: 1,
    logEveryIterations: 10,
    wsUpdateEveryIterations: 100,
    repetitionsPerSize: runtimeStudyRequest.repetitionsPerSize || 10,
    problemSizes: Array.isArray(runtimeStudyRequest.problemSizes)
      ? runtimeStudyRequest.problemSizes.join(", ")
      : "100, 200, 400, 800",
    wsUpdateEverySizes: 1,
  };
}

function getEdge(piece, direction) {
  const key = piece?.puzzleData?.logicalKey;
  if (!key) return null;

  switch (direction) {
    case "N":
      return Number(key[0]);
    case "E":
      return Number(key[1]);
    case "S":
      return Number(key[2]);
    case "W":
      return Number(key[3]);
    default:
      return null;
  }
}

function buildGridNeighbors(pieces, index, totalCols = GRID_COLUMNS) {
  const col = index % totalCols;
  const row = Math.floor(index / totalCols);

  const leftIndex = col > 0 ? index - 1 : null;
  const rightIndex = col < totalCols - 1 ? index + 1 : null;
  const topIndex = row > 0 ? index - totalCols : null;

  return {
    left:
      leftIndex === null ? { kind: "wall" }  : pieces[leftIndex]  ? { kind: "piece", edge: getEdge(pieces[leftIndex], "E") }  : { kind: "empty" },

    right:
      rightIndex === null  ? { kind: "wall" } : { kind: "empty" },

    top:
      topIndex === null ? { kind: "wall" }  : pieces[topIndex] ? { kind: "piece", edge: getEdge(pieces[topIndex], "S") } : { kind: "empty" },

    bottom: { kind: "empty" },
  };
}

export function flattenGroupedPuzzleConfig(groupedConfig) {
  const grouped = groupedConfig ?? createEmptyPuzzleConfig();

  return componentTypes.flatMap((type) =>
    (Array.isArray(grouped[type]) ? grouped[type] : []).map((piece) => ({
      id: piece.id,
      label: piece.label,
      type: piece.type ?? type,
      puzzleData: piece.puzzleData,
    }))
  );
}

export function normalizeStoredConfig(config, fallbackIndex = 0) {
  const base = {
    id: config?.id ?? `config-${fallbackIndex + 1}`,
    name: config?.name ?? `Config ${fallbackIndex + 1}`,
    params: config?.params ?? createEmptyParams(),
    tspInstance: config?.tspInstance ? cloneTspInstance(config.tspInstance) : null,
    vrpInstance: config?.vrpInstance ? cloneVrpInstance(config.vrpInstance) : null,
  };

  const placedPieces = Array.isArray(config?.placedPieces) ? config.placedPieces.map(clonePlacedPiece) : flattenGroupedPuzzleConfig(config?.puzzleConfig);

  return {
    ...base,
    placedPieces,
  };
}

export function rekeyGrid(pieces, _startIndex = 0, totalCols = GRID_COLUMNS) {
  const next = Array.isArray(pieces) ? [...pieces] : [];

  for (let i = 0; i < next.length; i++) {
    const col = i % totalCols;
    const row = Math.floor(i / totalCols);
    const neighbors = buildGridNeighbors(next, i, totalCols);

    next[i] = { ...next[i], puzzleData: generatePuzzleKey({  col, row, totalCols, neighbors, }), };
  }

  return next;
}

export function deriveGroupedPuzzleConfig(placedPieces) {
  const grouped = createEmptyPuzzleConfig();
  const pieces = Array.isArray(placedPieces) ? placedPieces : [];

  for (const piece of pieces) {
    if (!piece?.type || !grouped[piece.type]) continue;
    grouped[piece.type].push(piece);
  }

  return grouped;
}
// Apply a run template's request data to the puzzle configuration state, mapping component IDs to pieces and setting parameters accordingly.
export function applyTemplateRunRequestToState({
  runRequest,
  catalog,
  setPlacedPieces,
  setParams,
}) {
  if (!runRequest || !catalog) return;

  const componentMapping = [
    { type: "searchSpace", catalogKey: "searchSpaces", requestKey: "searchSpaceId", single: true },
    { type: "problem", catalogKey: "problems", requestKey: "problemIds" },
    { type: "generator", catalogKey: "generators", requestKey: "generatorId", single: true },
    { type: "selection", catalogKey: "selectionRules", requestKey: "selectionRuleId", single: true },
    { type: "populationModel", catalogKey: "populationModels", requestKey: "populationModelId", single: true },
    { type: "parentSelectionRule", catalogKey: "parentSelectionRules", requestKey: "parentSelectionRuleId", single: true },
    { type: "crossover", catalogKey: "crossovers", requestKey: "crossoverId", single: true },
    { type: "stopCondition", catalogKey: "stopConditions", requestKey: "stopConditionIds" },
    { type: "observer", catalogKey: "observers", requestKey: "observerIds" },
  ];

  const flattenedPieces = componentMapping.flatMap(
    ({ type, catalogKey, requestKey, single }) =>
      catalog[catalogKey]  ? mapIdsToPieces(runRequest[requestKey], catalog[catalogKey], type, single) : []
  );

  setPlacedPieces(rekeyGrid(flattenedPieces, 0));

  setParams({
    global: buildGlobalParamsFromRunRequest(runRequest),
    searchSpace: runRequest.searchSpaceParams || {},
    problem: runRequest.problemParams || {},
    generator: runRequest.generatorParams || {},
    selection: runRequest.selectionRuleParams || {},
    populationModel: runRequest.populationModelParams || {},
    parentSelectionRule: runRequest.parentSelectionRuleParams || {},
    crossover: runRequest.crossoverParams || {},
    stopCondition: runRequest.stopConditionParams || {},
    observer: {},
  });
}
// Similar to applyTemplateRunRequestToState but tailored for runtime study templates, which may have different parameter structures and typically do not include observers.
export function applyTemplateRuntimeStudyRequestToState({
  runtimeStudyRequest,
  catalog,
  setPlacedPieces,
  setParams,
}) {
  if (!runtimeStudyRequest || !catalog) return;

  const componentMapping = [
    { type: "searchSpace", catalogKey: "searchSpaces", requestKey: "searchSpaceId", single: true },
    { type: "problem", catalogKey: "problems", requestKey: "problemId", single: true },
    { type: "generator", catalogKey: "generators", requestKey: "generatorId", single: true },
    { type: "selection", catalogKey: "selectionRules", requestKey: "selectionRuleId", single: true },
    { type: "populationModel", catalogKey: "populationModels", requestKey: "populationModelId", single: true },
    { type: "parentSelectionRule", catalogKey: "parentSelectionRules", requestKey: "parentSelectionRuleId", single: true },
    { type: "crossover", catalogKey: "crossovers", requestKey: "crossoverId", single: true },
    { type: "stopCondition", catalogKey: "stopConditions", requestKey: "stopConditionIds" },
  ];

  const flattenedPieces = componentMapping.flatMap(
    ({ type, catalogKey, requestKey, single }) =>
      catalog[catalogKey]
        ? mapIdsToPieces(runtimeStudyRequest[requestKey], catalog[catalogKey], type, single)
        : []
  );

  setPlacedPieces(rekeyGrid(flattenedPieces, 0));

  setParams({
    global: buildGlobalParamsFromRuntimeStudyRequest(runtimeStudyRequest),
    searchSpace: runtimeStudyRequest.searchSpaceParams || {},
    problem: runtimeStudyRequest.problemParams || {},
    generator: runtimeStudyRequest.generatorParams || {},
    selection: runtimeStudyRequest.selectionRuleParams || {},
    populationModel: runtimeStudyRequest.populationModelParams || {},
    parentSelectionRule: runtimeStudyRequest.parentSelectionRuleParams || {},
    crossover: runtimeStudyRequest.crossoverParams || {},
    stopCondition: runtimeStudyRequest.stopConditionParams || {},
    observer: {},
  });
}