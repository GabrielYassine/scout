import { useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";
import { createOrderedProgressApplier } from "@/features/run/utils/orderedProgress.js";

function applyCompletedRuns(prev, completedRuns, summary) {
  if (!prev) {
    return { runId: null, batches: [], summary: summary ?? null };
  }

  const metaByKey = new Map(
    (completedRuns ?? []).map((item) => [`${item.runIndex}::${item.problemId}`, item])
  );

  const nextBatches = (prev.batches ?? []).map((batch) => ({
    ...batch,
    runs: (batch.runs ?? []).map((run) => {
      const meta = metaByKey.get(`${batch.runIndex}::${run.problemId}`);
      if (!meta) return run;

      return {
        ...run,
        runtimeMs: meta.runtimeMs,
      };
    }),
  }));

  return {
    ...prev,
    batches: nextBatches,
    summary: summary ?? prev.summary ?? null,
  };
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
    if (!enabled) return;
    if (!runId) return;

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

    const wsUrl = `${
      window.location.protocol === "https:" ? "wss" : "ws"
    }://${window.location.host}/ws`;

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
              runtimeMs: null,
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

    const flushProgressQueue = () => {
      if (!enabled) return;

      const queued = readyProgressQueueRef.current;
      if (!queued.length) return;

      readyProgressQueueRef.current = [];

      setLoading(false);
      setBatch((prev) => {
        let next = prev;
        for (const pkt of queued) {
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
          orderedApplierRef.current?.ingest(data);
          return;
        }

        if (data.type === "RUN_CONNECTED") {
          if (!startSentRef.current && runRequest) {
            startSentRef.current = true;
            client.publish({
              destination: `/app/run/${runId}/start`,
              body: JSON.stringify(runRequest),
            });
          }
          return;
        }

        if (data.type === "RUN_FINISHED") {
          flushProgressQueue();

          setLoading(false);
          setBatch((prev) => {
            const next = applyCompletedRuns(prev, data.completedRuns, data.summary);
            latestBatchRef.current = next;
            return next;
          });

          setSavedRun(() => ({
            pageMode: "run",
            runId,
            runRequest,
            batch: latestBatchRef.current,
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
          return;
        }

        if (data.type === "RUN_FAILED") {
          flushProgressQueue();
          setLoading(false);
          setError(data.message || "Run failed");
          client.deactivate();
        }
      });

      if (!readySentRef.current) {
        readySentRef.current = true;
        client.publish({
          destination: `/app/run/${runId}/ready`,
          body: JSON.stringify({ runId }),
        });
      }
    };

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
      } catch (e) {
        console.error("Failed to close run WebSocket", e);
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