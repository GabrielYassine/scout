/**
 * Manages the full websocket lifecycle for a normal run:
 * connects to the backend, listens for live run updates,
 * keeps the newest batch/run data in sync with React state and local storage,
 * merges incoming progress packets in the correct order,
 * updates loading/error state,
 * and closes the websocket when the run finishes, fails, or the component unmounts.
 */
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

// Merges new incoming value(s) into an existing list using the specified merge operation.
function mergeList(prevList, operation, incoming) {
  const prev = Array.isArray(prevList) ? prevList : [];
  switch (operation) {
    case "REPLACE":
      if (Array.isArray(incoming)) return [...incoming];
      if (incoming != null) return [incoming];
      return [];

    case "APPEND":
      if (Array.isArray(incoming)) return [...prev, ...incoming];
      if (incoming != null) return [...prev, incoming];
      return prev;

    case "REPLACE_LAST":
      if (Array.isArray(incoming)) return [...incoming];
      if (incoming == null) return prev;
      if (prev.length === 0) return [incoming];
      return [...prev.slice(0, -1), incoming];

    default:
      return prev;
  }
}
// Merges incoming series updates into the existing series object.
function mergeSeries(existingSeries, seriesDelta, seriesMerge) {
  const nextSeries = { ...(existingSeries ?? {}) };
  // Apply each delta operation to the corresponding series key.
  for (const [key, value] of Object.entries(seriesDelta ?? {})) {
    const operation = seriesMerge?.[key] ?? "APPEND";
    nextSeries[key] = mergeList(
      nextSeries[key],
      operation,
      value
    );
  }

  return nextSeries;
}
// Merges one incoming websocket progress update into the correct batch and run in frontend state.
function mergeProgress(prevBatch, update) {
  const base = prevBatch ?? createEmptyBatch(update.runId);
  const nextBatches = [...(base.batches ?? [])];
  // We first look for the batch that matches the incoming update's run index.
  const batchIndex = nextBatches.findIndex((batch) => batch.runIndex === update.runIndex);

  // Reuse the existing batch if found, otherwise create a new one.
  const nextBatch =
    batchIndex >= 0
      ? { ...nextBatches[batchIndex] }
      : { runIndex: update.runIndex, seed: update.seed, runs: [] };

  const nextRuns = [...(nextBatch.runs ?? [])];
  // Find the run for the specific problem being updated.
  const existingRunIndex = nextRuns.findIndex((run) => run.problemId === update.problemId);
  // Reuse the existing run if it already exists.
  // Otherwise create a fresh run object.
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

  // Merge evaluation data into the run.
  nextRun.evaluations = mergeList(
    nextRun.evaluations,
    update.evaluationsMerge,
    update.evaluations ?? update.evaluation
  );
  // Merge all incoming series updates (fitness, bestFitness, etc.).
  nextRun.series = mergeSeries(nextRun.series, update.seriesDelta, update.seriesMerge);

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

function createWebSocketUrl() {
  const protocol = window.location.protocol === "https:" ? "wss" : "ws";
  return `${protocol}://${window.location.host}/ws`;
}
// Safely parses incoming WebSocket messages, returning null for invalid JSON.
function parseSocketMessage(rawMessage) {
  try {
    return JSON.parse(rawMessage.body);
  } catch {
    return null;
  }
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
    // Processes all progress updates that have been waiting in the queue.
    const flushProgressQueue = () => {
      if (!enabled || readyProgressQueueRef.current.length === 0) return;

      const queuedPackets = readyProgressQueueRef.current;
      readyProgressQueueRef.current = [];

      let nextBatch = latestBatchRef.current ?? null;
      // We merge all queued packets in the correct order to produce the latest batch state.
      for (const packet of queuedPackets) {
        if (!packet?.runId) continue;
        if (activeRunIdRef.current && packet.runId !== activeRunIdRef.current) continue;
        nextBatch = mergeProgress(nextBatch, packet);
      }

      latestBatchRef.current = nextBatch;
      setLoading(false);
      setBatch(nextBatch);
    };
   // Sends the "ready" signal to the backend to indicate that the client is prepared to receive updates.
    const handleRunConnected = () => {
      if (startSentRef.current || !runRequest) return;

      startSentRef.current = true;
      client.publish({
        destination: `/app/run/${runId}/start`,
        body: JSON.stringify(runRequest),
      });
    };
    // Handles the completion of the run by flushing any remaining progress updates,
    // updating the batch with final summary data, and saving the run state.
    const handleRunFinished = (message) => {
      flushProgressQueue();

      const finishedBatch = {
        ...(latestBatchRef.current ?? createEmptyBatch(message.runId ?? runId)),
        runId: message.runId ?? runId,
        searchSpaceId: message.searchSpaceId ?? null,
        summary: message.summary ?? null,
      };

      latestBatchRef.current = finishedBatch;
      setLoading(false);
      setBatch(finishedBatch);

      setSavedRun({
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
      });

      client.deactivate();
    };
    // Handles run failure by flushing any remaining progress updates, setting an error message, and closing the WebSocket connection.
    const handleRunFailed = (message) => {
      flushProgressQueue();
      setLoading(false);
      setError(message.message || "Run failed");
      client.deactivate();
    };
    // Main handler for incoming WebSocket messages.
    // It parses the message, checks if it pertains to the active run, and routes it to the appropriate handler based on the message type.
    const handleSocketMessage = (rawMessage) => {
      const message = parseSocketMessage(rawMessage);
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

      if (readySentRef.current) return;

      readySentRef.current = true;
      client.publish({
        destination: `/app/run/${runId}/ready`,
        body: JSON.stringify({ runId }),
      });
    };
    // We set up a timer to flush incoming progress updates at a regular interval.
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