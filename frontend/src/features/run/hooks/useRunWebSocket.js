import { useEffect } from "react";
import { Client } from "@stomp/stompjs";

import { normalizeBatch, normalizeSeriesMap } from "@/features/run/utils/runData.js";

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
}) {
  useEffect(() => {
    if (!enabled) return;
    if (!runId) return;

    const wsUrl = `${window.location.protocol === "https:" ? "wss" : "ws"}://${window.location.host}/ws`;

    const client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 0,
    });

    const appendSeriesValue = (series, key, value) => {
      if (value === undefined) return series;

      const next = { ...series };
      const list = Array.isArray(next[key]) ? [...next[key]] : [];

      if (key === "tspCities") {
        next[key] = [value];
        return next;
      }

      if (key === "tspTour" || key === "pheromoneHeatmap") {
        list.push(value);
        next[key] = list;
        return next;
      }

      if (Array.isArray(value)) {
        list.push(value);
        next[key] = list;
        return next;
      }

      list.push(value);
      next[key] = list;
      return next;
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

      if (Array.isArray(update.iterations)) {
        run.iterations = [...update.iterations];
      } else if (update.iteration != null) {
        run.iterations = [...run.iterations, update.iteration];
      }

      if (Array.isArray(update.evaluations)) {
        run.evaluations = [...update.evaluations];
      } else if (update.evaluation != null) {
        run.evaluations = [...run.evaluations, update.evaluation];
      }

      const seriesDelta = update.seriesDelta ?? {};
      const seriesValues = Object.values(seriesDelta);
      const hasSeriesSnapshot = seriesValues.length > 0 && seriesValues.every(Array.isArray);

      if (hasSeriesSnapshot) {
        run.series = normalizeSeriesMap(seriesDelta);
      } else {
        run.series = Object.entries(seriesDelta).reduce(
          (acc, [key, value]) => appendSeriesValue(acc, key, value),
          run.series ?? {}
        );
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

    client.onConnect = () => {
      client.subscribe(`/topic/run/${runId}`, (message) => {
        const data = JSON.parse(message.body);

        if (data.type === "RUN_PROGRESS") {
          setLoading(false);
          setBatch((prev) => mergeProgress(prev, data));
          return;
        }

        if (data.type === "RUN_CONNECTED") {
          console.log("Run WebSocket connected", { runId: data.runId, message: data.message });
          return;
        }

        if (data.type === "RUN_FINISHED") {
          console.log("Run WebSocket finished", { runId: data.runId, message: data.message });
          setLoading(false);
          const normalized = normalizeBatch(data.batch ?? null);
          setBatch(normalized);
          setSavedRun({
            pageMode: "run",
            batch: normalized,
            studyPoints: [],
            loading: false,
            puzzleConfig,
            params,
            tspInstance,
            vrpInstance,
            selectedRunKey: 0,
            savedAt: Date.now(),
          });
          client.deactivate();
          return;
        }

        if (data.type === "RUN_FAILED") {
          setLoading(false);
          setError(data.message || "Run failed");
          client.deactivate();
          return;
        }

        if (data.type === "RUN_DISCONNECTED") {
          console.log("Run WebSocket disconnected", { runId: data.runId, message: data.message });
          client.deactivate();
        }
      });

      client.publish({
        destination: `/app/run/${runId}/connect`,
        body: JSON.stringify({}),
      });
    };

    client.activate();

    return () => {
      try {
        if (client.connected) {
          client.publish({
            destination: `/app/run/${runId}/disconnect`,
            body: JSON.stringify({}),
          });
        }
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
  ]);
}
