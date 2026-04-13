import { useMemo, useState, useRef, useCallback } from "react";
import { useLocation, useNavigate } from "react-router-dom";

import LabLeftbar from "@/shared/components/sidebars/LabLeftbar.jsx";
import LabRightbar from "@/shared/components/sidebars/LabRightbar.jsx";
import RunChart from "@/features/run/components/charts/RunChart.jsx";
import RuntimeStudyChart from "@/features/run/components/charts/RuntimeStudyChart.jsx";

import "./RunPage.css";

import { useLocalStorageState } from "@/shared/hooks/useLocalStorageState.js";

import {
  computeAnimationLength,
  normalizeBatch,
  normalizeSelectedRunKey,
} from "@/features/run/utils/runData.js";
import { usePlayback } from "@/features/run/hooks/usePlayback.js";
import { useRunWebSocket } from "@/features/run/hooks/useRunWebSocket.js";
import { useRuntimeStudyWebSocket } from "@/features/run/hooks/useRuntimeStudyWebSocket.js";
import { startRun } from "@/shared/api/run.js";

export default function RunPage({ catalog, catalogLoading, catalogError }) {
  const location = useLocation();
  const navigate = useNavigate();
  const [savedRun, setSavedRun] = useLocalStorageState("scout:lastRun", null);

  const locationState = location.state ?? {};
  const hasIncomingExecution =
    Boolean(locationState.runId) ||
    Boolean(locationState.studyId) ||
    locationState.loading === true;

  const restoredRun = hasIncomingExecution ? null : savedRun;

  const pageMode =
    locationState.pageMode ??
    (locationState.runId ? "run" : null) ??
    (locationState.studyId ? "runtimeStudy" : null) ??
    restoredRun?.pageMode ??
    "run";
  const runId = locationState.runId ?? null;
  const studyId = locationState.studyId ?? null;

  const batchResponse = locationState.batch ?? restoredRun?.batch ?? null;
  const initialLoading = locationState.loading ?? restoredRun?.loading ?? false;
  const initialError = locationState.error ?? null;
  const puzzleConfig = locationState.puzzleConfig ?? restoredRun?.puzzleConfig ?? [];
  const runtimeStudyRequest =
    locationState.runtimeStudyRequest ?? restoredRun?.runtimeStudyRequest ?? null;
  const params = locationState.params ?? restoredRun?.params ?? [];
  const initialTspInstance = locationState.tspInstance ?? restoredRun?.tspInstance ?? null;
  const initialVrpInstance = locationState.vrpInstance ?? restoredRun?.vrpInstance ?? null;

  const [batch, setBatch] = useState(() => normalizeBatch(batchResponse ?? null));
  const [studyPoints, setStudyPoints] = useState(() => restoredRun?.studyPoints ?? []);
  const [loading, setLoading] = useState(!!initialLoading);
  const [error, setError] = useState(initialError ?? null);

  const batches = batch?.batches ?? [];
  const averageByProblem = batch?.summary?.averageByProblem ?? {};
  const bestFitnessBoxPlotsByProblem = batch?.summary?.bestFitnessBoxPlotsByProblem ?? {};
  const averageRunTimeByProblem = batch?.summary?.averageRunTimeByProblem ?? {};

  const averageRuns = useMemo(
    () =>
      Object.entries(averageByProblem).map(([problemId, avg]) => ({
        problemId,
        iterations: avg.iterations ?? [],
        evaluations: avg.evaluations ?? [],
        series: avg.series ?? {},
        runtimeMs: averageRunTimeByProblem[problemId] ?? null,
        isAverage: true,
      })),
    [averageByProblem, averageRunTimeByProblem]
  );

  const [selectedRunKey, setSelectedRunKey] = useState(() => {
    if (restoredRun?.selectedRunKey != null) {
      return restoredRun.selectedRunKey;
    }
    return Object.keys(averageByProblem).length > 0 ? "average" : "0";
  });

  function handleSelectedRunChange(value) {
    setSelectedRunKey(value);
    setSavedRun((prev) =>
      prev
        ? {
            ...prev,
            selectedRunKey: value,
          }
        : prev
    );
  }

  const effectiveSelectedRunKey = normalizeSelectedRunKey(
    selectedRunKey,
    averageRuns,
    batches
  );

  const selectedBatch =
    effectiveSelectedRunKey === "average" ? null : batches[Number(effectiveSelectedRunKey)];

  const runs = effectiveSelectedRunKey === "average" ? averageRuns : selectedBatch?.runs ?? [];

  const tspInstance = initialTspInstance;
  const vrpInstance = initialVrpInstance;

  const runtimeStudyProblemId =
    location.state?.runtimeStudyRequest?.problemId ?? puzzleConfig?.problem?.[0]?.id ?? null;

  const currentAnimationLength = useMemo(
    () => computeAnimationLength({ pageMode, studyPoints, runs }),
    [pageMode, studyPoints, runs]
  );

  const { playbackSpeed, setPlaybackSpeed, visibleCount, resetPlayback } = usePlayback({
    length: currentAnimationLength,
    initialSpeed: 50,
  });

  const startSentRef = useRef(false);

  const sessionId = useMemo(() => {
    const key = "scout:sessionId";
    const existing = window.sessionStorage?.getItem(key);
    if (existing) return existing;
    const next = window.crypto?.randomUUID
      ? window.crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    try {
      window.sessionStorage?.setItem(key, next);
    } catch {
      // ignore
    }
    return next;
  }, []);

  const onRunWsReady = useCallback(async () => {
    if (startSentRef.current) return;
    if (pageMode !== "run") return;
    if (!runId) return;

    startSentRef.current = true;

    try {
      setLoading(true);
      setError(null);

      const runTimes = params.global?.runTimes ?? 1;
      const logEveryIterations = params.global?.logEveryIterations ?? 100;
      const wsUpdateEveryIterations = params.global?.wsUpdateEveryIterations ?? 100;

      const searchSpaceParams = { ...(params.searchSpace ?? {}) };
      const problemParams = { ...(params.problem ?? {}) };

      const problemList = Array.isArray(puzzleConfig.problem) ? puzzleConfig.problem : [];
      const isTSPProblem = problemList.some((p) => p.id === "tsp");
      const isVrpProblem = problemList.some((p) => p.id === "vrp");

      if (isTSPProblem && tspInstance?.cities?.length) {
        problemParams.tspInstance = tspInstance;
        searchSpaceParams.n = tspInstance.cities.length;
      }

      if (isVrpProblem && vrpInstance) {
        problemParams.vrpInstance = vrpInstance;
        searchSpaceParams.vrpInstance = vrpInstance;
      }

      const body = {
        searchSpaceId: puzzleConfig.searchSpace?.[0]?.id ?? null,
        searchSpaceParams,
        problemIds: puzzleConfig.problem?.map((x) => x.id) ?? [],
        problemParams,
        generatorId: puzzleConfig.generator?.[0]?.id ?? null,
        generatorParams: params.generator ?? {},
        selectionRuleId: puzzleConfig.selection?.[0]?.id ?? null,
        selectionRuleParams: params.selection ?? {},
        populationModelId: puzzleConfig.populationModel?.[0]?.id ?? null,
        populationModelParams: params.populationModel ?? {},
        parentSelectionRuleId: puzzleConfig.parentSelectionRule?.[0]?.id ?? null,
        parentSelectionRuleParams: params.parentSelectionRule ?? {},
        crossoverId: puzzleConfig.crossover?.[0]?.id ?? null,
        crossoverParams: params.crossover ?? {},
        stopConditionIds: puzzleConfig.stopCondition?.map((x) => x.id) ?? [],
        stopConditionParams: params.stopCondition ?? {},
        observerIds: puzzleConfig.observer?.map((x) => x.id) ?? [],
        observerParams: params.observer ?? {},
        seed: params.global?.seed ?? Date.now(),
        runTimes,
        sessionId,
        runId,
        logEveryIterations,
        wsUpdateEveryIterations,
      };

      await startRun(body);
      // Keep loading state until we receive progress/finished/failed.
    } catch (e) {
      startSentRef.current = false;
      setLoading(false);
      setError(e?.message || "Failed to start run");
    }
  }, [pageMode, runId, params, puzzleConfig, tspInstance, vrpInstance, sessionId]);

  useRunWebSocket({
    enabled: pageMode === "run",
    runId,
    puzzleConfig,
    params,
    tspInstance,
    vrpInstance,
    setLoading,
    setError,
    setBatch,
    setSavedRun,
    onReady: onRunWsReady,
  });

  useRuntimeStudyWebSocket({
    enabled: pageMode === "runtimeStudy",
    studyId,
    runtimeStudyRequest,
    puzzleConfig,
    params,
    tspInstance,
    vrpInstance,
    setLoading,
    setError,
    setStudyPoints,
    setSavedRun,
  });

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
        ) : (
          <>
            {!loading && !error && currentAnimationLength > 0 && (
              <div className="run-speed-control">
                <label htmlFor="playback-speed" className="field-label">
                  Graph speed:
                </label>
                <input
                  id="playback-speed"
                  className="field-input"
                  type="range"
                  min="1"
                  max="200"
                  value={playbackSpeed}
                  onChange={(e) => setPlaybackSpeed(Number(e.target.value))}
                />
                <span>{playbackSpeed}</span>
                <button
                  type="button"
                  className="reset-playback-button"
                  onClick={resetPlayback}
                >
                  Reset
                </button>
              </div>
            )}

            {pageMode === "runtimeStudy" ? (
              studyPoints.length === 0 ? (
                <div className="run-chart-panel">
                  <div className="run-chart-title">No runtime study data</div>
                  <div>No study points to plot.</div>
                </div>
              ) : (
                <RuntimeStudyChart
                  studyTitle="Runtime Study"
                  problemId={runtimeStudyProblemId}
                  points={studyPoints}
                  visibleCount={visibleCount}
                />
              )
            ) : batches.length === 0 ? (
              <div className="run-chart-panel">
                <div className="run-chart-title">No run data</div>
                <div>No data to plot.</div>
              </div>
            ) : (
              <>
                {(averageRuns.length > 0 || batches.length > 1) && (
                  <div className="run-selector">
                    <label htmlFor="batch-select" className="field-label">
                      Select Run:
                    </label>
                    <select
                      id="batch-select"
                      className="field-input"
                      value={effectiveSelectedRunKey}
                      onChange={(e) => handleSelectedRunChange(e.target.value)}
                    >
                      {averageRuns.length > 0 && <option value="average">Average</option>}
                      {batches.map((batchItem, idx) => (
                        <option key={idx} value={String(idx)}>
                          Run {batchItem.runIndex} (Seed: {batchItem.seed})
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                <div className="run-stack">
                  {runs.map((run, idx) => (
                    <RunChart
                      key={`${effectiveSelectedRunKey}-${idx}`}
                      run={run}
                      runIndex={selectedBatch?.runIndex ?? "average"}
                      visibleCount={visibleCount}
                      bestFitnessBoxPlot={
                        effectiveSelectedRunKey === "average"
                          ? bestFitnessBoxPlotsByProblem[run.problemId] ?? null
                          : null
                      }
                    />
                  ))}
                </div>
              </>
            )}
          </>
        )}
      </div>

      <div className="run-sidebar run-sidebar--disabled" aria-disabled="true">
        <LabRightbar
          hoverInfo={null}
          tspInstance={tspInstance}
          vrpInstance={vrpInstance}
          onTspInstanceChange={() => {}}
          onVrpInstanceChange={() => {}}
        />
      </div>
    </div>
  );
}
