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

export const cloneTspInstance = (tsp) =>
  tsp
    ? {
        ...tsp,
        cities: (tsp.cities ?? []).map((c) => ({ ...c })),
      }
    : null;

export const cloneVrpInstance = (vrp) =>
  vrp
    ? {
        ...vrp,
        depot: vrp.depot ? { ...vrp.depot } : null,
        customers: (vrp.customers ?? []).map((c) => ({ ...c })),
      }
    : null;

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

  if (Array.isArray(config?.placedPieces)) {
    return {
      ...base,
      placedPieces: config.placedPieces.map((piece) => ({
        id: piece.id,
        label: piece.label,
        type: piece.type,
        puzzleData: piece.puzzleData,
      })),
    };
  }

  return {
    ...base,
    placedPieces: flattenGroupedPuzzleConfig(config?.puzzleConfig),
  };
}

function getEdge(piece, direction) {
  const key = piece?.puzzleData?.logicalKey;
  if (!key) return null;

  switch (direction) {
    case "N":
      return parseInt(key[0], 10);
    case "E":
      return parseInt(key[1], 10);
    case "S":
      return parseInt(key[2], 10);
    case "W":
      return parseInt(key[3], 10);
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
  const bottomIndex = index + totalCols < pieces.length ? index + totalCols : null;

  return {
    left:
      leftIndex === null
        ? { kind: "wall" }
        : pieces[leftIndex]
          ? { kind: "piece", edge: getEdge(pieces[leftIndex], "E") }
          : { kind: "empty" },

    right:
      rightIndex === null
        ? { kind: "wall" }
        : pieces[rightIndex]
          ? { kind: "piece", edge: getEdge(pieces[rightIndex], "W") }
          : { kind: "empty" },

    top:
      topIndex === null
        ? { kind: "wall" }
        : pieces[topIndex]
          ? { kind: "piece", edge: getEdge(pieces[topIndex], "S") }
          : { kind: "empty" },

    bottom:
      bottomIndex === null
        ? { kind: "empty" }
        : pieces[bottomIndex]
          ? { kind: "piece", edge: getEdge(pieces[bottomIndex], "N") }
          : { kind: "empty" },
  };
}

export function rekeyGrid(pieces, startIndex = 0, totalCols = GRID_COLUMNS) {
  const next = Array.isArray(pieces) ? [...pieces] : [];

  for (let i = Math.max(0, startIndex); i < next.length; i++) {
    const col = i % totalCols;
    const row = Math.floor(i / totalCols);
    const neighbors = buildGridNeighbors(next, i, totalCols);

    next[i] = {
      ...next[i],
      puzzleData: generatePuzzleKey({
        col,
        row,
        totalCols,
        neighbors,
      }),
    };
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

export function applyTemplateRunRequestToState({ runRequest, catalog, setPlacedPieces, setParams }) {
  if (!runRequest || !catalog) return;

  const normalizeIds = (value, single) => {
    if (value == null) return [];
    const ids = Array.isArray(value) ? value : [value];
    return single ? ids.slice(0, 1) : ids;
  };

  const mapIdsToPieces = (ids, catalogItems, pieceType, single = false) =>
    normalizeIds(ids, single)
      .map((id) => {
        const item = catalogItems.find((x) => x.id === id);
        return item ? { id: item.id, label: item.displayName, type: pieceType } : null;
      })
      .filter(Boolean);

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

  const flattenedPieces = componentMapping.flatMap(({ type, catalogKey, requestKey, single }) =>
    catalog[catalogKey] ? mapIdsToPieces(runRequest[requestKey], catalog[catalogKey], type, single) : []
  );

  setPlacedPieces(rekeyGrid(flattenedPieces, 0));

  setParams({
    global: {
      experimentType: "run",
      seed: runRequest.seed || Date.now(),
      runTimes: runRequest.runTimes || 1,
      logEveryIterations: runRequest.logEveryIterations || 100,
      wsUpdateEveryIterations: runRequest.wsUpdateEveryIterations || 100,
      problemSizes: "100, 200, 400, 800",
      repetitionsPerSize: 10,
      wsUpdateEverySizes: 1,
    },
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
