export function createEmptyBatch(runId = null) {
  return {
    runId,
    batches: [],
    summary: null,
  };
}

export function mergeList(prevList, operation, incomingSingle, incomingList) {
  const prev = Array.isArray(prevList) ? prevList : [];

  switch (operation) {
    case "REPLACE":
      if (Array.isArray(incomingList)) return [...incomingList];
      if (incomingSingle != null) return [incomingSingle];
      return [];

    case "APPEND":
      if (Array.isArray(incomingList)) return [...prev, ...incomingList];
      if (incomingSingle != null) return [...prev, incomingSingle];
      return prev;

    case "REPLACE_LAST":
      if (Array.isArray(incomingList)) return [...incomingList];
      if (incomingSingle == null) return prev;
      if (prev.length === 0) return [incomingSingle];
      return [...prev.slice(0, -1), incomingSingle];

    default:
      return prev;
  }
}

export function mergeSeries(existingSeries, seriesDelta, seriesMerge) {
  const nextSeries = { ...(existingSeries ?? {}) };

  for (const [key, value] of Object.entries(seriesDelta ?? {})) {
    const operation = seriesMerge?.[key] ?? "APPEND";

    nextSeries[key] = mergeList(
      nextSeries[key],
      operation,
      value,
      Array.isArray(value) ? value : null
    );
  }

  return nextSeries;
}

export function mergeProgress(prevBatch, update) {
  const base = prevBatch ?? createEmptyBatch(update.runId);
  const nextBatches = [...(base.batches ?? [])];

  const batchIndex = nextBatches.findIndex(
    (batch) => batch.runIndex === update.runIndex
  );

  const nextBatch =
    batchIndex >= 0
      ? { ...nextBatches[batchIndex] }
      : { runIndex: update.runIndex, seed: update.seed, runs: [] };

  const nextRuns = [...(nextBatch.runs ?? [])];

  const existingRunIndex = nextRuns.findIndex(
    (run) => run.problemId === update.problemId
  );

  const nextRun =
    existingRunIndex >= 0
      ? { ...nextRuns[existingRunIndex] }
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
    update.evaluation,
    update.evaluations
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

  nextRuns[existingRunIndex >= 0 ? existingRunIndex : nextRuns.length] = nextRun;
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