import { useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";

export function useRunWebSocket({
  enabled,
  runId,
  puzzleConfig,
  params,
  tspInstance,
  vrpInstance,
  setLoading,
  setError,
  setBatch,
  setSavedRun,
  onReady,
}) {
  const activeRunIdRef = useRef(null);
  const readyFiredRef = useRef(false);

  const progressQueueRef = useRef([]);
  const flushTimerRef = useRef(null);
  const latestBatchRef = useRef(null);

  useEffect(() => {
    if (!enabled) return;
    if (!runId) return;

    activeRunIdRef.current = runId;
    readyFiredRef.current = false;
    progressQueueRef.current = [];
    latestBatchRef.current = null;

    const wsUrl = `${window.location.protocol === "https:" ? "wss" : "ws"}://${window.location.host}/ws`;

    const client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 0,
    });

    const FLUSH_INTERVAL_MS = 75;

    const mergeList = (prevList, op, incomingSingle, incomingList) => {
      const prev = Array.isArray(prevList) ? prevList : [];
      if (!op) return prev;

      if (op === "REPLACE") {
        if (Array.isArray(incomingList)) return [...incomingList];
        if (incomingSingle != null) return [incomingSingle];
        return [];
      }

      if (op === "APPEND") {
        if (Array.isArray(incomingList)) return [...prev, ...incomingList];
        if (incomingSingle != null) return [...prev, incomingSingle];
        return prev;
      }

      if (op === "REPLACE_LAST") {
        if (Array.isArray(incomingList)) return [...incomingList];
        if (incomingSingle == null) return prev;
        if (prev.length === 0) return [incomingSingle];
        const next = [...prev];
        next[next.length - 1] = incomingSingle;
        return next;
      }

      return prev;
    };

    const mergeProgress = (prev, update) => {
      const base = prev ?? { runId: update.runId, batches: [], summary: null };
      const nextBatches = [...(base.batches ?? [])];

      const batchIndex = nextBatches.findIndex((b) => b.runIndex === update.runIndex);
      const runGroup =
        batchIndex >= 0
          ? { ...nextBatches[batchIndex] }
          : { runIndex: update.runIndex, seed: update.seed, runs: [] };

      const runsList = [...(runGroup.runs ?? [])];
      const runIndex = runsList.findIndex((r) => r.problemId === update.problemId);

      const run =
        runIndex >= 0
          ? { ...runsList[runIndex] }
          : {
              problemId: update.problemId,
              iterations: [],
              evaluations: [],
              series: {},
              runtimeMs: 0,
              finalEvaluations: 0,
            };

      run.iterations = mergeList(
        run.iterations,
        update.iterationsMerge,
        update.iteration,
        update.iterations
      );
      run.evaluations = mergeList(
        run.evaluations,
        update.evaluationsMerge,
        update.evaluation,
        update.evaluations
      );

      const seriesDelta = update.seriesDelta ?? {};
      const seriesMerge = update.seriesMerge ?? {};

      const nextSeries = { ...(run.series ?? {}) };
      for (const [key, value] of Object.entries(seriesDelta)) {
        const op = seriesMerge[key] ?? "APPEND";
        nextSeries[key] = mergeList(
          nextSeries[key],
          op,
          value,
          Array.isArray(value) ? value : null
        );
      }
      run.series = nextSeries;

      const xLen = Math.min(
        run.evaluations.length,
        run.iterations.length || run.evaluations.length
      );

      for (const [key, op] of Object.entries(seriesMerge)) {
        if (op === "APPEND") {
          const arr = run.series?.[key];
          if (Array.isArray(arr) && arr.length > xLen) {
            run.series[key] = arr.slice(0, xLen);
          }
        }
      }

      runsList[runIndex >= 0 ? runIndex : runsList.length] = run;
      runGroup.runs = runsList;

      if (batchIndex >= 0) {
        nextBatches[batchIndex] = runGroup;
      } else {
        nextBatches.push(runGroup);
      }

      return { ...base, batches: nextBatches };
    };

    const sortBySequence = (packets) =>
      [...packets].sort((a, b) => {
        const aSeq = a.sequenceId ?? 0;
        const bSeq = b.sequenceId ?? 0;
        return aSeq - bSeq;
      });

    const flushProgressQueue = () => {
      if (!enabled) return;
      const queued = progressQueueRef.current;
      if (!queued.length) return;

      progressQueueRef.current = [];
      const orderedPackets = sortBySequence(queued);

      setLoading(false);
      setBatch((prev) => {
        let next = prev;
        for (const pkt of orderedPackets) {
          if (!pkt?.runId) continue;
          if (activeRunIdRef.current && pkt.runId !== activeRunIdRef.current) continue;
          next = mergeProgress(next, pkt);
        }
        latestBatchRef.current = next;
        return next;
      });
    };

    flushTimerRef.current = setInterval(flushProgressQueue, FLUSH_INTERVAL_MS);

    client.onConnect = () => {
      activeRunIdRef.current = runId;

      client.subscribe(`/topic/run/${runId}`, (message) => {
        let data;
        try {
          data = JSON.parse(message.body);
        } catch {
          return;
        }

        if (!data?.runId) return;
        if (activeRunIdRef.current && data.runId !== activeRunIdRef.current) return;

        if (data.type === "RUN_PROGRESS") {
          progressQueueRef.current.push(data);
          return;
        }

        if (data.type === "RUN_CONNECTED") {
          return;
        }

        if (data.type === "RUN_FINISHED") {
          // Flush any outstanding progress before marking complete.
          flushProgressQueue();

          console.log("Run WebSocket finished", { runId: data.runId, message: data.message });
          setLoading(false);

          // Terminal packet can add end-only information (e.g., summary/averages) without replacing batches.
          if (data.summary) {
            setBatch((prev) => {
              const base = prev ?? { runId: data.runId, batches: [], summary: null };
              const next = { ...base, summary: data.summary };
              latestBatchRef.current = next;
              return next;
            });
          }

          setSavedRun(() => ({
            pageMode: "run",
            batch: latestBatchRef.current,
            studyPoints: [],
            loading: false,
            puzzleConfig,
            params,
            tspInstance,
            vrpInstance,
            selectedRunKey: 0,
            savedAt: Date.now(),
          }));

          client.deactivate();
          return;
        }

        if (data.type === "RUN_FAILED") {
          flushProgressQueue();
          setLoading(false);
          setError(data.message || "Run failed");
          client.deactivate();
        }
      });

      client.publish({
        destination: `/app/run/${runId}/connect`,
        body: JSON.stringify({}),
      });

      if (!readyFiredRef.current) {
        readyFiredRef.current = true;
        try {
          onReady?.();
        } catch (e) {
          console.error("onReady handler failed", e);
        }
      }
    };

    client.activate();

    return () => {
      try {
        if (flushTimerRef.current) {
          clearInterval(flushTimerRef.current);
          flushTimerRef.current = null;
        }

        progressQueueRef.current = [];
        latestBatchRef.current = null;
        client.deactivate();
      } catch (e) {
        console.error("Failed to close run WebSocket", e);
      }
    };
  }, [
    enabled,
    runId,
    puzzleConfig,
    params,
    tspInstance,
    vrpInstance,
    setLoading,
    setError,
    setBatch,
    setSavedRun,
    onReady,
  ]);
}