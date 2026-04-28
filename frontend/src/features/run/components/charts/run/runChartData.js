/**
 * Pure helpers for RunChart observer selection, phase ranges, status labels,
 * and incremental chart-point construction.
 */
export const HYPERCUBE_KEY = "__hypercube__";
export const TSP_TOUR_KEY = "__tsp-tour__";
export const BEST_FITNESS_BOXPLOT_KEY = "__boxplot__:bestFitness";

const SPECIAL_SERIES_KEYS = new Set([
  "hypercubeX",
  "hypercubeY",
  "tspTour",
  "tspCities",
  "pheromoneHeatmap",
  "fitnessPhaseIntervals",
]);

export function getRunStatusMeta(run) {
  const rawStatus = String(run?.status ?? "").toUpperCase();

  if (rawStatus === "FINISHED") {
    return { label: "Finished", className: "finished" };
  }

  if (rawStatus === "FAILED") {
    return { label: "Failed", className: "failed" };
  }

  return { label: "Running", className: "ongoing" };
}

export function getObserverDisplayName(observerKey) {
  switch (observerKey) {
    case HYPERCUBE_KEY:
      return "Hypercube";
    case TSP_TOUR_KEY:
      return "TSP Tour";
    case BEST_FITNESS_BOXPLOT_KEY:
      return "bestFitness boxplot";
    default:
      return observerKey;
  }
}

export function getLineSeriesKeys(series) {
  return Object.keys(series).filter((key) => !SPECIAL_SERIES_KEYS.has(key));
}

export function buildDisplayKeys({
  lineSeriesKeys,
  hasHypercube,
  hasTspTour,
  hasBestFitnessBoxPlot,
}) {
  const keys = [...lineSeriesKeys];

  if (hasHypercube) keys.push(HYPERCUBE_KEY);
  if (hasTspTour) keys.push(TSP_TOUR_KEY);
  if (hasBestFitnessBoxPlot) keys.push(BEST_FITNESS_BOXPLOT_KEY);

  return keys;
}

export function getFallbackObserver(displayKeys) {
  if (displayKeys.length > 0) return displayKeys[0];
  return null;
}

function normalizePhaseRanges(phaseIntervals) {
  return (phaseIntervals ?? [])
    .map((interval) => {
      const startEval = Number(interval?.startEvaluation);
      const endEval = Number(interval?.endEvaluation);

      if (!Number.isFinite(startEval) || !Number.isFinite(endEval)) {
        return null;
      }

      return {
        start: Math.min(startEval, endEval),
        end: Math.max(startEval, endEval),
        phase: typeof interval?.phase === "string" ? interval.phase : "STAGNANT",
      };
    })
    .filter(Boolean)
    .sort((a, b) => a.start - b.start);
}

function resolveRangeGapsAndOverlaps(sortedRanges) {
  const resolvedRanges = [];

  for (const range of sortedRanges) {
    const previous = resolvedRanges[resolvedRanges.length - 1];

    if (previous && range.start > previous.end) {
      resolvedRanges.push({
        start: previous.end,
        end: range.start,
        phase: previous.phase,
      });
    }

    if (previous && range.start < previous.end) {
      if (range.end <= previous.end) {
        continue;
      }

      resolvedRanges.push({
        ...range,
        start: previous.end,
      });
      continue;
    }

    if (range.end > range.start) {
      resolvedRanges.push(range);
    }
  }

  return resolvedRanges;
}

export function buildPhaseRanges(phaseIntervals) {
  if (!phaseIntervals?.length) {
    return [];
  }

  const sortedRanges = normalizePhaseRanges(phaseIntervals);
  return resolveRangeGapsAndOverlaps(sortedRanges);
}

export function createEmptyPointCache(observerKey) {
  return {
    observerKey,
    lastEvalLen: 0,
    lastYLen: 0,
    points: [],
  };
}

export function buildChartPoints({
  effectiveObserver,
  evaluations,
  series,
  isStandardLineSeries,
  pointsCacheRef,
}) {
  if (!isStandardLineSeries) {
    pointsCacheRef.current = createEmptyPointCache(effectiveObserver);
    return [];
  }

  const observerValues = series[effectiveObserver] ?? [];
  const evaluationLength = evaluations.length;
  const valueLength = observerValues.length;
  const sharedLength = Math.min(evaluationLength, valueLength);

  const cache = pointsCacheRef.current;
  const sameObserver = cache.observerKey === effectiveObserver;

  const shouldRebuild =
    !sameObserver ||
    cache.lastEvalLen > evaluationLength ||
    cache.lastYLen > valueLength ||
    cache.points.length > sharedLength;

  if (shouldRebuild) {
    const rebuiltPoints = [];

    for (let i = 0; i < sharedLength; i += 1) {
      const x = Number(evaluations[i]);
      const y = Number(observerValues[i]);

      if (Number.isFinite(x) && Number.isFinite(y)) {
        rebuiltPoints.push([x, y]);
      }
    }

    pointsCacheRef.current = {
      observerKey: effectiveObserver,
      lastEvalLen: evaluationLength,
      lastYLen: valueLength,
      points: rebuiltPoints,
    };

    return rebuiltPoints;
  }

  const startIndex = cache.points.length;

  if (startIndex >= sharedLength) {
    cache.lastEvalLen = evaluationLength;
    cache.lastYLen = valueLength;
    return cache.points;
  }

  const nextPoints = cache.points.slice();

  for (let i = startIndex; i < sharedLength; i += 1) {
    const x = Number(evaluations[i]);
    const y = Number(observerValues[i]);

    if (Number.isFinite(x) && Number.isFinite(y)) {
      nextPoints.push([x, y]);
    }
  }

  pointsCacheRef.current = {
    observerKey: effectiveObserver,
    lastEvalLen: evaluationLength,
    lastYLen: valueLength,
    points: nextPoints,
  };

  return nextPoints;
}