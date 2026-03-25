import { useState, useEffect, useMemo } from "react";
import { Client } from "@stomp/stompjs";
import { useLocation, useNavigate } from "react-router-dom";
import LabLeftbar from "../components/LabLeftbar/LabLeftbar.jsx";
import LabRightbar from "../components/LabRightbar.jsx";
import RunChart from "../components/charts/RunChart.jsx";
import { DEFAULT_TSP_INSTANCE } from "../contexts/PuzzleConfigContext.jsx";
import "./RunPage.css";
import "../components/LabLeftbar/LabLeftbar.css";
import "../components/LabRightbar.css";

export default function RunPage({ catalog, catalogLoading, catalogError }) {
  const location = useLocation();
  const navigate = useNavigate();

  const runId = location.state?.runId;

  const batchResponse = location.state?.batch;
  const initialLoading = location.state?.loading;
  const initialError = location.state?.error;
  const puzzleConfig = location.state?.puzzleConfig ?? [];
  const params = location.state?.params ?? [];
  const initialTspInstance = location.state?.tspInstance ?? DEFAULT_TSP_INSTANCE;

  const normalizeSeriesMap = (series) => {
    if (!series) return {};
    return Object.fromEntries(
      Object.entries(series).map(([key, value]) => {
        if (value && typeof value === "object" && Array.isArray(value.values)) {
          return [key, value.values];
        }
        if (Array.isArray(value)) {
          return [key, value];
        }
        return [key, []];
      })
    );
  };

  const normalizeBatch = (incoming) => {
    if (!incoming) return incoming;
    const batches = (incoming.batches ?? []).map((batchItem) => ({
      ...batchItem,
      runs: (batchItem.runs ?? []).map((run) => ({
        ...run,
        series: normalizeSeriesMap(run.series),
      })),
    }));
    return { ...incoming, batches };
  };

  const [batch, setBatch] = useState(() => normalizeBatch(batchResponse ?? null));
  const [loading, setLoading] = useState(!!initialLoading);
  const [error, setError] = useState(initialError ?? null);

  const batches = batch?.batches ?? [];
  const averageByProblem = batch?.summary?.averageByProblem ?? {};
  const averageRuns = useMemo(
    () =>
      Object.entries(averageByProblem).map(([problemId, avg]) => ({
        problemId,
        iterations: avg.iterations ?? [],
        evaluations: avg.evaluations ?? [],
        series: avg.series ?? {},
        isAverage: true,
      })),
    [averageByProblem]
  );

  const [selectedRunKey, setSelectedRunKey] = useState(
    Object.keys(averageByProblem).length > 0 ? "average" : "0"
  );

  useEffect(() => {
    if (selectedRunKey === "average" && averageRuns.length === 0 && batches.length > 0) {
      setSelectedRunKey("0");
    }
  }, [selectedRunKey, averageRuns.length, batches.length]);

  const selectedBatch =
    selectedRunKey === "average" ? null : batches[Number(selectedRunKey)];

  const runs = selectedRunKey === "average" ? averageRuns : selectedBatch?.runs ?? [];

  const [tspInstance] = useState(initialTspInstance);

  useEffect(() => {
    if (!runId) {
      console.log("No runId provided, skipping WebSocket connection");
      return;
    }

    const wsUrl = `${window.location.protocol === "https:" ? "wss" : "ws"}://${window.location.host}/ws`;
    console.log("Connecting to WebSocket at", wsUrl, "for runId", runId);

    const client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 0,
    });

    const appendSeriesValue = (series, key, value) => {
      if (value === undefined) return series;
      const next = { ...series };
      const list = Array.isArray(next[key]) ? [...next[key]] : [];
      if (key === "tspCities") {
        if (list.length === 0) list.push(value);
      } else {
        list.push(value);
      }
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

      run.iterations = [...run.iterations, update.iteration];
      run.evaluations = [...run.evaluations, update.evaluation];
      run.series = Object.entries(update.seriesDelta ?? {}).reduce(
        (acc, [key, value]) => appendSeriesValue(acc, key, value),
        run.series ?? {}
      );

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
      console.log("WebSocket connected", runId);
      client.subscribe(`/topic/run/${runId}`, (message) => {
        const data = JSON.parse(message.body);

        if (data.type === "RUN_PROGRESS") {
          setLoading(false);
          setBatch((prev) => mergeProgress(prev, data));
          return;
        }

        if (data.type === "RUN_FINISHED") {
          setLoading(false);
          setBatch(normalizeBatch(data.batch ?? null));
          client.deactivate();
          return;
        }

        if (data.type === "RUN_FAILED") {
          setLoading(false);
          setError(data.message || "Run failed");
          client.deactivate();
        }

        if (data.type === "RUN_DISCONNECTED") {
          client.deactivate();
        }
      });

      client.publish({
        destination: `/app/run/${runId}/connect`,
        body: JSON.stringify({}),
      });
    };

    client.onStompError = (frame) => {
      console.error("WebSocket STOMP error", frame.headers["message"], frame.body);
    };

    client.onWebSocketError = (event) => {
      console.error("WebSocket transport error", event);
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
        console.error("Failed to close WebSocket", e);
      }
    };
  }, [runId]);

  return (
    <div className="run-page">
      <div className="run-sidebar run-sidebar--disabled" aria-disabled="true">
        <LabLeftbar
          puzzleConfig={puzzleConfig}
          params={params}
          onParamChange={() => {}}
          onReset={() => navigate("/lab")}
          onRun={() => navigate("/lab")}
          catalog={catalog}
          catalogLoading={catalogLoading}
          catalogError={catalogError}
          readOnly
        />
      </div>

      <div className="run-page-content">
        {loading ? (
          <div className="run-loading">
            <div className="spinner" aria-label="Loading" />
            <div>Preparing run...</div>
          </div>
        ) : error ? (
          <div className="run-chart-panel">
            <div className="run-chart-title">Run failed</div>
            <div>{error}</div>
          </div>
        ) : batches.length === 0 ? (
          <div className="run-chart-panel">
            <div className="run-chart-title">No run data</div>
            <div>No data to plot..</div>
          </div>
        ) : (
          <>
            {(averageRuns.length > 0 || batches.length > 1) && (
              <div className="run-selector">
                <label htmlFor="batch-select" className="form-label">
                  Select Run:
                </label>
                <select
                  id="batch-select"
                  className="form-select"
                  value={selectedRunKey}
                  onChange={(e) => setSelectedRunKey(e.target.value)}
                >
                  {averageRuns.length > 0 && (
                    <option value="average">Average</option>
                  )}

                  {batches.map((batch, idx) => (
                    <option key={idx} value={String(idx)}>
                      Run {batch.runIndex} (Seed: {batch.seed})
                    </option>
                  ))}
                </select>
              </div>
            )}

            <div className="run-stack">
              {runs.map((run, idx) => (
                <RunChart
                  key={`${selectedRunKey}-${idx}`}
                  run={run}
                  runIndex={selectedBatch?.runIndex ?? "average"}
                  problemIndex={idx + 1}
                />
              ))}
            </div>
          </>
        )}
      </div>
      <div className="run-sidebar run-sidebar--disabled" aria-disabled="true">
        <LabRightbar
          hoverInfo={null}
          tspInstance={tspInstance}
          onTspInstanceChange={() => {}}
        />
      </div>
    </div>
  );
}
