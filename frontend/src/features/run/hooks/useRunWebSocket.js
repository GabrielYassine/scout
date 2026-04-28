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
import {
  createEmptyBatch,
  mergeProgress,
} from "@/features/run/utils/runProgressMerge.js";
import {
  createWebSocketUrl,
  parseSocketMessage,
} from "@/features/run/utils/socketUtils.js";

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
      if (!enabled || readyProgressQueueRef.current.length === 0) return;

      const queuedPackets = readyProgressQueueRef.current;
      readyProgressQueueRef.current = [];

      let nextBatch = latestBatchRef.current ?? null;

      for (const packet of queuedPackets) {
        if (!packet?.runId) continue;
        if (activeRunIdRef.current && packet.runId !== activeRunIdRef.current) {
          continue;
        }

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

    const handleRunFailed = (message) => {
      flushProgressQueue();
      setLoading(false);
      setError(message.message || "Run failed");
      client.deactivate();
    };

    const handleSocketMessage = (rawMessage) => {
      const message = parseSocketMessage(rawMessage);

      if (!message?.runId) return;
      if (activeRunIdRef.current && message.runId !== activeRunIdRef.current) {
        return;
      }

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