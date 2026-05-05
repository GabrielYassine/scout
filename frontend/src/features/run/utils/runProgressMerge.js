/**
 * Pure helpers for merging websocket progress packets into the frontend batch model.
 * These functions do not mutate the previous batch state.
 */
export function createEmptyBatch(runId = null) {
  return {
    runId,
    batches: [],
    summary: null,
  };
}

function asIncomingValues(value) {
  if (value == null) {
    return [];
  }

  return Array.isArray(value) ? value : [value];
}

export function mergeList(previousList, operation = "APPEND", incomingValue) {
  const previous = Array.isArray(previousList) ? previousList : [];
  const incoming = asIncomingValues(incomingValue);

  switch (operation) {
    case "REPLACE":
      return incoming;

    case "APPEND":
      return [...previous, ...incoming];

    case "REPLACE_LAST":
      if (incoming.length === 0) {
        return previous;
      }

      if (previous.length === 0) {
        return incoming;
      }

      return [...previous.slice(0, -1), incoming[incoming.length - 1]];

    default:
      throw new Error(`Unsupported merge operation: ${operation}`);
  }
}

export function mergeSeries(existingSeries, seriesDelta, seriesMerge) {
  const nextSeries = { ...(existingSeries ?? {}) };

  // Each observer series can define its own merge strategy.
  // Normal series usually append, while latest-only visual series can replace.
  for (const [key, value] of Object.entries(seriesDelta ?? {})) {
    const operation = seriesMerge?.[key] ?? "APPEND";
    const incomingValue = Array.isArray(value) ? [value] : value;
    nextSeries[key] = mergeList(nextSeries[key], operation, incomingValue);
  }

  return nextSeries;
}

export function mergeProgress(previousBatch, update) {
  const base = previousBatch ?? createEmptyBatch(update.runId);
  const nextBatches = [...(base.batches ?? [])];

  const batchIndex = nextBatches.findIndex(
    (batch) => batch.runIndex === update.runIndex
  );

  const nextBatch =
    batchIndex >= 0
      ? { ...nextBatches[batchIndex] }
      : { runIndex: update.runIndex, seed: update.seed, runs: [] };

  const nextRuns = [...(nextBatch.runs ?? [])];

  const runIndex = nextRuns.findIndex(
    (run) => run.problemId === update.problemId
  );

  const nextRun =
    runIndex >= 0
      ? { ...nextRuns[runIndex] }
      : {
          problemId: update.problemId,
          searchSpaceId: update.searchSpaceId,
          evaluations: [],
          series: {},
          runtimeMs: null,
          finalEvaluations: 0,
          status: "ONGOING",
        };

  if (update.searchSpaceId != null) {
    nextRun.searchSpaceId = update.searchSpaceId;
  }

  nextRun.evaluations = mergeList(
    nextRun.evaluations,
    update.evaluationsMerge,
    update.evaluations ?? update.evaluation
  );

  nextRun.series = mergeSeries(
    nextRun.series,
    update.seriesDelta,
    update.seriesMerge
  );

  if (typeof update.runtimeMs === "number" && Number.isFinite(update.runtimeMs)) {
    nextRun.runtimeMs = update.runtimeMs;
  }

  if (typeof update.status === "string" && update.status.trim() !== "") {
    nextRun.status = update.status;
  }

  nextRuns[runIndex >= 0 ? runIndex : nextRuns.length] = nextRun;
  nextBatch.runs = nextRuns;

  if (batchIndex >= 0) {
    nextBatches[batchIndex] = nextBatch;
  } else {
    nextBatches.push(nextBatch);
  }

  return {
    ...base,
    batches: nextBatches,
  };
}