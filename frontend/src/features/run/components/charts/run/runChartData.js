/**
 * Helpers for run chart series and observer data.
 * @author s235257 & s230632
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

/**
 * Returns ordinary numeric observer series that can be rendered as line charts.
 * Special series are consumed by custom visualizations instead.
 */
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

export function getInitialObserver(displayKeys) {
  return displayKeys[0] ?? null;
}

export function isSpecialObserver(observerKey) {
  return (
    observerKey === HYPERCUBE_KEY ||
    observerKey === TSP_TOUR_KEY ||
    observerKey === BEST_FITNESS_BOXPLOT_KEY
  );
}

export function isMinimizationFitnessObserver(observerKey, searchSpaceId) {
  return (
    (observerKey === "fitness" || observerKey === "bestFitness") &&
    (searchSpaceId === "permutation" || searchSpaceId === "route-list")
  );
}

export function invertFitnessPoints(points) {
  return points.map(([x, y]) => [x, -y]);
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

/**
 * Makes phase ranges continuous and non-overlapping.
 * Gaps inherit the previous phase so the chart background does not break
 * between adjacent observer intervals.
 */
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

/**
 * Builds [evaluation, value] points for a line chart.
 * During live runs, new points are appended to a cache instead of rebuilding
 * the entire point array every render.
 */
export function buildChartPoints({
  observerKey,
  evaluations,
  series,
  isLineSeries,
  pointsCacheRef,
}) {
  if (!isLineSeries) {
    pointsCacheRef.current = createEmptyPointCache(observerKey);
    return [];
  }

  const observerValues = series[observerKey];
  const sharedLength = Math.min(evaluations.length, observerValues.length);
  const cache = pointsCacheRef.current;

  const shouldRebuild =
    cache.observerKey !== observerKey ||
    cache.lastEvalLen > evaluations.length ||
    cache.lastYLen > observerValues.length ||
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
      observerKey,
      lastEvalLen: evaluations.length,
      lastYLen: observerValues.length,
      points: rebuiltPoints,
    };

    return rebuiltPoints;
  }

  if (cache.points.length >= sharedLength) {
    cache.lastEvalLen = evaluations.length;
    cache.lastYLen = observerValues.length;
    return cache.points;
  }

  const nextPoints = cache.points.slice();

  for (let i = cache.points.length; i < sharedLength; i += 1) {
    const x = Number(evaluations[i]);
    const y = Number(observerValues[i]);

    if (Number.isFinite(x) && Number.isFinite(y)) {
      nextPoints.push([x, y]);
    }
  }

  pointsCacheRef.current = {
    observerKey,
    lastEvalLen: evaluations.length,
    lastYLen: observerValues.length,
    points: nextPoints,
  };

  return nextPoints;
}