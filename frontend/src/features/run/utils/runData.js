/**
 * Pure helpers for run data (normalization, selection).
 */

export const normalizeSeriesMap = (series) => {
  if (!series) return {};
  return Object.fromEntries(
    Object.entries(series).map(([key, value]) => {
      if (value && typeof value === "object" && Array.isArray(value.values)) {
        return [key, value.values];
      }
      if (Array.isArray(value)) {
        return [key, value];
      }
      return [key, []];
    })
  );
};

export const normalizeBatch = (incoming) => {
  if (!incoming) return incoming;
  const batches = (incoming.batches ?? []).map((batchItem) => ({
    ...batchItem,
    runs: (batchItem.runs ?? []).map((run) => ({
      ...run,
      series: normalizeSeriesMap(run.series),
    })),
  }));
  return { ...incoming, batches };
};

export function normalizeSelectedRunKey(selectedKey, averageRuns, batches) {
  const hasAverage = averageRuns.length > 0;
  const numericIndex = Number(selectedKey);
  if (!Number.isInteger(numericIndex) || numericIndex < 0 || numericIndex >= batches.length) {
    return hasAverage ? "average" : "0";
  }
  return String(numericIndex);
}

export function computeAnimationLength({ pageMode, studyPoints, runs }) {
  if (pageMode === "runtimeStudy") {
    return studyPoints.length;
  }

  if (!runs.length) return 0;

  return Math.max(
    ...runs.map((run) => {
      const series = run?.series ?? {};

      const hypercubeLen = Math.min(series.hypercubeX?.length ?? 0, series.hypercubeY?.length ?? 0);
      const tspLen = series.tspTour?.length ?? 0;

      const normalLens = Object.values(series)
        .filter(Array.isArray)
        .map((arr) => arr.length);

      return Math.max(hypercubeLen, tspLen, ...normalLens, run.evaluations?.length ?? 0);
    })
  );
}
