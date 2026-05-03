/**
 * Manages the websocket lifecycle for normal runs.
 * Incoming progress packets are buffered, merged into batch state,
 * and persisted when the run finishes.
 */
import { useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";

import { createEmptyBatch, mergeProgress } from "@/features/run/utils/runProgressMerge.js";
import { createWebSocketUrl, parseSocketMessage } from "@/features/run/utils/socketUtils.js";

// Progress packets may arrive frequently, so UI state is updated in small batches
// instead of once per websocket message.
// This interval is a tradeoff between UI responsiveness and React rendering overhead.
// But is a must since it will be too expensive otherwise.
const FLUSH_INTERVAL_MS = 75;

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
  setStreaming,
}) {
  // Refs are used for websocket lifecycle data that must stay current without
  // causing React re-renders for every incoming packet.
  const activeRunIdRef = useRef(null);
  const startSentRef = useRef(false);
  const flushTimerRef = useRef(null);
  const latestBatchRef = useRef(null);
  const readyProgressQueueRef = useRef([]);

  useEffect(() => {
    if (!enabled || !runId) return;

    // Reset per-run lifecycle state when a new live run is connected.
    activeRunIdRef.current = runId;
    startSentRef.current = false;
    latestBatchRef.current = null;
    readyProgressQueueRef.current = [];

    const client = new Client({
      brokerURL: createWebSocketUrl(),
      reconnectDelay: 0,
    });

    client.onWebSocketClose = (event) => {
      console.log("[Run WS] disconnected", {
        runId,
        code: event.code,
        reason: event.reason,
      });
    };

    const flushProgressQueue = () => {
      if (!enabled || readyProgressQueueRef.current.length === 0) return;

      const queuedPackets = readyProgressQueueRef.current;
      readyProgressQueueRef.current = [];

      let nextBatch = latestBatchRef.current ?? null;

      // Merge all queued progress packets into one batch update. This avoids
      // triggering a React state update for every single websocket message.
      for (const packet of queuedPackets) {
        if (!packet.runId) continue;
        if (activeRunIdRef.current && packet.runId !== activeRunIdRef.current) {
          continue;
        }

        nextBatch = mergeProgress(nextBatch, packet);
      }

      latestBatchRef.current = nextBatch;
      setLoading(false);
      setBatch(nextBatch);
    };

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
      setStreaming(false);
      setBatch(finishedBatch);

      // Persist the finished run so the RunPage can be restored after refresh
      // or navigation without reconnecting to the websocket.
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

    const handleRunFailed = (message) => {
      flushProgressQueue();
      setLoading(false);
      setStreaming(false);
      setError(message.message || "Run failed");
      client.deactivate();
    };

    const handleSocketMessage = (rawMessage) => {
      const message = parseSocketMessage(rawMessage);

      if (!message.runId) return;
      if (activeRunIdRef.current && message.runId !== activeRunIdRef.current) {
        return;
      }

      switch (message.type) {
        case "RUN_PROGRESS":
          readyProgressQueueRef.current.push(message);
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

      if (startSentRef.current || !runRequest) return;

      // Start the run after subscribing so progress packets are not missed.
      startSentRef.current = true;
      client.publish({
        destination: `/app/run/${runId}/start`,
        body: JSON.stringify({
          sessionId: runRequest.sessionId,
        }),
      });
    };

    flushTimerRef.current = setInterval(flushProgressQueue, FLUSH_INTERVAL_MS);
    client.activate();

    return () => {
      try {
        if (flushTimerRef.current) {
          clearInterval(flushTimerRef.current);
          flushTimerRef.current = null;
        }

        // Clear run-local buffers so stale packets cannot affect a later run.
        readyProgressQueueRef.current = [];
        latestBatchRef.current = null;
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
    setStreaming,
  ]);
}