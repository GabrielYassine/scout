import { useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";
import { createOrderedProgressApplier } from "@/features/run/utils/orderedProgress.js";

const FLUSH_INTERVAL_MS = 75;

function createEmptyBatch(runId = null) {
  return {
    runId,
    batches: [],
    summary: null,
  };
}

function applyCompletedRuns(prevBatch, completedRuns, summary, runId) {
  const base = prevBatch ?? createEmptyBatch(runId);

  const completedByKey = new Map(
    (completedRuns ?? []).map((item) => [`${item.runIndex}::${item.problemId}`, item])
  );

  const batches = (base.batches ?? []).map((batch) => ({
    ...batch,
    runs: (batch.runs ?? []).map((run) => {
      const completed = completedByKey.get(`${batch.runIndex}::${run.problemId}`);
      if (!completed) return run;

      return {
        ...run,
        runtimeMs: completed.runtimeMs,
        status: "FINISHED",
      };
    }),
  }));

  return {
    ...base,
    runId: base.runId ?? runId ?? null,
    batches,
    summary: summary ?? base.summary ?? null,
  };
}

function mergeList(prevList, operation, incomingSingle, incomingList) {
  const prev = Array.isArray(prevList) ? prevList : [];
  if (!operation) return prev;

  if (operation === "REPLACE") {
    if (Array.isArray(incomingList)) return [...incomingList];
    if (incomingSingle != null) return [incomingSingle];
    return [];
  }

  if (operation === "APPEND") {
    if (Array.isArray(incomingList)) return [...prev, ...incomingList];
    if (incomingSingle != null) return [...prev, incomingSingle];
    return prev;
  }

  if (operation === "REPLACE_LAST") {
    if (Array.isArray(incomingList)) return [...incomingList];
    if (incomingSingle == null) return prev;
    if (prev.length === 0) return [incomingSingle];

    const next = [...prev];
    next[next.length - 1] = incomingSingle;
    return next;
  }

  return prev;
}

function mergeSeries(existingSeries, seriesDelta, seriesMerge) {
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

function trimAppendedSeriesToXAxisLength(run, seriesMerge) {
  const xLength = Math.min(
    run.evaluations.length,
    run.iterations.length || run.evaluations.length
  );

  for (const [key, operation] of Object.entries(seriesMerge ?? {})) {
    if (operation !== "APPEND") continue;

    const values = run.series?.[key];
    if (Array.isArray(values) && values.length > xLength) {
      run.series[key] = values.slice(0, xLength);
    }
  }
}

function mergeProgress(prevBatch, update) {
  const base = prevBatch ?? createEmptyBatch(update.runId);
  const nextBatches = [...(base.batches ?? [])];

  const batchIndex = nextBatches.findIndex((batch) => batch.runIndex === update.runIndex);
  const nextBatch =
    batchIndex >= 0
      ? { ...nextBatches[batchIndex] }
      : { runIndex: update.runIndex, seed: update.seed, runs: [] };

  const nextRuns = [...(nextBatch.runs ?? [])];
  const runIndex = nextRuns.findIndex((run) => run.problemId === update.problemId);

  const nextRun =
    runIndex >= 0
      ? { ...nextRuns[runIndex] }
      : {
          problemId: update.problemId,
          searchSpaceId: update.searchSpaceId,
          iterations: [],
          evaluations: [],
          series: {},
          runtimeMs: null,
          finalEvaluations: 0,
          status: "ONGOING",
        };
  if (update.searchSpaceId != null) {
    nextRun.searchSpaceId = update.searchSpaceId;
  }
  nextRun.iterations = mergeList(
    nextRun.iterations,
    update.iterationsMerge,
    update.iteration,
    update.iterations
  );

  nextRun.evaluations = mergeList(
    nextRun.evaluations,
    update.evaluationsMerge,
    update.evaluation,
    update.evaluations
  );

  nextRun.series = mergeSeries(nextRun.series, update.seriesDelta, update.seriesMerge);
  trimAppendedSeriesToXAxisLength(nextRun, update.seriesMerge);
  nextRun.status = "ONGOING";

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

function createWebSocketUrl() {
  const protocol = window.location.protocol === "https:" ? "wss" : "ws";
  return `${protocol}://${window.location.host}/ws`;
}

export function useRunWebSocket({
  enabled,
  runId,
  runRequest,
  puzzleConfig,
  params,
  tspInstance,
  vrpInstance,
  setLoading,
  setError,
  setBatch,
  setSavedRun,
}) {
  const activeRunIdRef = useRef(null);
  const readySentRef = useRef(false);
  const startSentRef = useRef(false);

  const flushTimerRef = useRef(null);
  const latestBatchRef = useRef(null);
  const readyProgressQueueRef = useRef([]);
  const orderedApplierRef = useRef(null);

  useEffect(() => {
    if (!enabled || !runId) return;

    activeRunIdRef.current = runId;
    readySentRef.current = false;
    startSentRef.current = false;
    latestBatchRef.current = null;
    readyProgressQueueRef.current = [];

    orderedApplierRef.current = createOrderedProgressApplier({
      applyPacket: (packet) => {
        readyProgressQueueRef.current.push(packet);
      },
    });

    const client = new Client({
      brokerURL: createWebSocketUrl(),
      reconnectDelay: 0,
    });

    const flushProgressQueue = () => {
      if (!enabled) return;

      const queuedPackets = readyProgressQueueRef.current;
      if (!queuedPackets.length) return;

      readyProgressQueueRef.current = [];

      let nextBatch = latestBatchRef.current ?? null;

      for (const packet of queuedPackets) {
        if (!packet?.runId) continue;
        if (activeRunIdRef.current && packet.runId !== activeRunIdRef.current) continue;
        nextBatch = mergeProgress(nextBatch, packet);
      }

      latestBatchRef.current = nextBatch;
      setLoading(false);
      setBatch(nextBatch);
    };

    const handleRunConnected = () => {
      if (startSentRef.current || !runRequest) return;

      startSentRef.current = true;
      client.publish({
        destination: `/app/run/${runId}/start`,
        body: JSON.stringify(runRequest),
      });
    };

    const handleRunFinished = (message) => {
      flushProgressQueue();

      const finishedBatch ={
      ...applyCompletedRuns(
        latestBatchRef.current,
        message.completedRuns,
        message.summary,
        message.runId ?? runId
      ),
      searchSpaceId: message.searchSpaceId ?? null,
    };

      latestBatchRef.current = finishedBatch;
      setLoading(false);
      setBatch(finishedBatch);

      setSavedRun(() => ({
        pageMode: "run",
        runId,
        runRequest,
        batch: finishedBatch,
        studyPoints: [],
        loading: false,
        puzzleConfig,
        params,
        tspInstance,
        vrpInstance,
        selectedRunKey: "0",
        savedAt: Date.now(),
      }));

      client.deactivate();
    };

    const handleRunFailed = (message) => {
      flushProgressQueue();
      setLoading(false);
      setError(message.message || "Run failed");
      client.deactivate();
    };

    const handleSocketMessage = (rawMessage) => {
      let message;
      try {
        message = JSON.parse(rawMessage.body);
      } catch {
        return;
      }

      if (!message?.runId) return;
      if (activeRunIdRef.current && message.runId !== activeRunIdRef.current) return;

      switch (message.type) {
        case "RUN_PROGRESS":
          orderedApplierRef.current?.ingest(message);
          return;

        case "RUN_CONNECTED":
          handleRunConnected();
          return;

        case "RUN_FINISHED":
          handleRunFinished(message);
          return;

        case "RUN_FAILED":
          handleRunFailed(message);
          return;

        default:
          return;
      }
    };

    client.onConnect = () => {
      activeRunIdRef.current = runId;

      client.subscribe(`/topic/run/${runId}`, handleSocketMessage);

      if (!readySentRef.current) {
        readySentRef.current = true;
        client.publish({
          destination: `/app/run/${runId}/ready`,
          body: JSON.stringify({ runId }),
        });
      }
    };

    flushTimerRef.current = setInterval(flushProgressQueue, FLUSH_INTERVAL_MS);
    client.activate();

    return () => {
      try {
        if (flushTimerRef.current) {
          clearInterval(flushTimerRef.current);
          flushTimerRef.current = null;
        }

        readyProgressQueueRef.current = [];
        latestBatchRef.current = null;
        orderedApplierRef.current?.resetAll?.();
        client.deactivate();
      } catch {
        // ignore cleanup errors
      }
    };
  }, [
    enabled,
    runId,
    runRequest,
    puzzleConfig,
    params,
    tspInstance,
    vrpInstance,
    setLoading,
    setError,
    setBatch,
    setSavedRun,
  ]);
}