function normalizeSeriesValues(value) {
  if (value && typeof value === "object" && Array.isArray(value.values)) {
    return value.values;
  }

  if (Array.isArray(value)) {
    return value;
  }

  return [];
}

export function normalizeSeriesMap(seriesMap) {
  if (!seriesMap) return {};

  return Object.fromEntries(
    Object.entries(seriesMap).map(([key, value]) => [key, normalizeSeriesValues(value)])
  );
}

function normalizeRun(run) {
  return {
    ...run,
    series: normalizeSeriesMap(run.series),
  };
}

function normalizeBatchItem(batchItem) {
  return {
    ...batchItem,
    runs: (batchItem.runs ?? []).map(normalizeRun),
  };
}

export function normalizeBatch(batch) {
  if (!batch) return batch;

  return {
    ...batch,
    batches: (batch.batches ?? []).map(normalizeBatchItem),
  };
}

export function normalizeSelectedRunKey(selectedKey, averageRuns, batches) {
  const numericKey = Number(selectedKey);
  const hasAverage = averageRuns.length > 0;

  const isValidBatchIndex =
    Number.isInteger(numericKey) &&
    numericKey >= 0 &&
    numericKey < batches.length;

  if (isValidBatchIndex) {
    return String(numericKey);
  }

  return hasAverage ? "average" : "0";
}

function getRunAnimationLength(run) {
  const series = run?.series ?? {};

  const hypercubeLength = Math.min(
    series.hypercubeX?.length ?? 0,
    series.hypercubeY?.length ?? 0
  );

  const tspTourLength = series.tspTour?.length ?? 0;

  const standardSeriesLengths = Object.values(series)
    .filter(Array.isArray)
    .map((values) => values.length);

  return Math.max(
    hypercubeLength,
    tspTourLength,
    run.evaluations?.length ?? 0,
    ...standardSeriesLengths
  );
}

export function computeAnimationLength({ pageMode, studyPoints, runs }) {
  if (pageMode === "runtimeStudy") {
    return studyPoints.length;
  }

  if (runs.length === 0) {
    return 0;
  }

  return Math.max(...runs.map(getRunAnimationLength));
}