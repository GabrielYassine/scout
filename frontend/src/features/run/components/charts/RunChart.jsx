import { useState, useMemo, memo, useCallback, useEffect, useRef } from "react";
import "./RunChart.css";
import HypercubePlot from "./HypercubePlot.jsx";
import TSPVisualization from "./RouteVisualization/RouteVisualization.jsx";
import LineCharts, { LineChartStatsPanel } from "./LineCharts.jsx";
import BoxPlotChart from "./BoxPlotChart.jsx";

const HYPERCUBE_KEY = "__hypercube__";
const TSP_TOUR_KEY = "__tsp-tour__";
const BEST_FITNESS_BOXPLOT_KEY = "__boxplot__:bestFitness";

function RunChart({ run, runIndex, visibleCount, instanceName = null, bestFitnessBoxPlot = null }) {
  const evaluations = run?.evaluations ?? [];
  const iterations = run?.iterations ?? [];
  const series = run?.series ?? {};
  const hasHypercube = (series.hypercubeX?.length ?? 0) > 0 && (series.hypercubeY?.length ?? 0) > 0;
  const hasTSPTour = (series.tspTour?.length ?? 0) > 0 && (series.tspCities?.length ?? 0) > 0;
  const runtimeMs = Number.isFinite(run?.runtimeMs) ? run.runtimeMs : null;

  const keys = Object.keys(series).filter(
    (k) =>
      k !== "hypercubeX" &&
      k !== "hypercubeY" &&
      k !== "tspTour" &&
      k !== "tspCities" &&
      k !== "pheromoneHeatmap" &&
      k !== "fitnessPhaseIntervals"
  );

  const displayKeys = useMemo(() => {
    const out = [...keys];
    if (hasHypercube) out.push(HYPERCUBE_KEY);
    if (hasTSPTour) out.push(TSP_TOUR_KEY);
    if (bestFitnessBoxPlot) out.push(BEST_FITNESS_BOXPLOT_KEY);
    return out;
  }, [keys, hasHypercube, hasTSPTour, bestFitnessBoxPlot]);

  const fallbackObserver = useMemo(
    () => displayKeys[0] || (hasTSPTour ? TSP_TOUR_KEY : null),
    [displayKeys, hasTSPTour]
  );
  const [selectedObserver, setSelectedObserver] = useState(fallbackObserver);
  const effectiveObserver = displayKeys.includes(selectedObserver)
    ? selectedObserver
    : fallbackObserver;

  const handleObserverChange = useCallback((observerKey) => {
    setSelectedObserver(observerKey);
  }, []);

  const isBestFitnessBoxPlot = effectiveObserver === BEST_FITNESS_BOXPLOT_KEY;

  const pointsCacheRef = useRef({
    observerKey: null,
    lastEvalLen: 0,
    lastYLen: 0,
    points: [],
  });

  const data = useMemo(() => {
    if (
      !effectiveObserver ||
      effectiveObserver === HYPERCUBE_KEY ||
      effectiveObserver === TSP_TOUR_KEY ||
      isBestFitnessBoxPlot ||
      !series[effectiveObserver]
    ) {
      // Reset cache when we're not plotting a standard line series.
      pointsCacheRef.current = {
        observerKey: effectiveObserver,
        lastEvalLen: 0,
        lastYLen: 0,
        points: [],
      };
      return [];
    }

    const observerData = series[effectiveObserver] ?? [];
    const evalLen = evaluations.length;
    const yLen = observerData.length;
    const minLen = Math.min(evalLen, yLen);

    const cache = pointsCacheRef.current;
    const sameObserver = cache.observerKey === effectiveObserver;

    // Detect reset/shrink or observer switch => rebuild from scratch.
    const shouldRebuild =
      !sameObserver ||
      cache.lastEvalLen > evalLen ||
      cache.lastYLen > yLen ||
      cache.points.length > minLen;

    if (shouldRebuild) {
      const next = [];
      for (let i = 0; i < minLen; i += 1) {
        const x = Number(evaluations[i]);
        const y = Number(observerData[i]);
        if (Number.isFinite(x) && Number.isFinite(y)) next.push([x, y]);
      }
      pointsCacheRef.current = {
        observerKey: effectiveObserver,
        lastEvalLen: evalLen,
        lastYLen: yLen,
        points: next,
      };
      return next;
    }

    // Incremental append from cached length up to new minLen.
    const start = cache.points.length;
    if (start >= minLen) {
      // Nothing new.
      cache.lastEvalLen = evalLen;
      cache.lastYLen = yLen;
      return cache.points;
    }

    const next = cache.points.slice();
    for (let i = start; i < minLen; i += 1) {
      const x = Number(evaluations[i]);
      const y = Number(observerData[i]);
      if (Number.isFinite(x) && Number.isFinite(y)) next.push([x, y]);
    }

    pointsCacheRef.current = {
      observerKey: effectiveObserver,
      lastEvalLen: evalLen,
      lastYLen: yLen,
      points: next,
    };

    return next;
  }, [effectiveObserver, evaluations, series, isBestFitnessBoxPlot]);

  const visibleData = useMemo(() => {
    return data.slice(0, visibleCount);
  }, [data, visibleCount]);

  const [lineChartWindowRange, setLineChartWindowRange] = useState(null);

  const statsVisiblePoints = useMemo(() => {
    if (!lineChartWindowRange) return visibleData;
    return visibleData.filter(([x]) => x >= lineChartWindowRange.min && x <= lineChartWindowRange.max);
  }, [visibleData, lineChartWindowRange]);

  const phaseRanges = useMemo(() => {
    const intervals = series.fitnessPhaseIntervals ?? [];
    if (!intervals.length || !iterations.length || !evaluations.length) return [];

    const iterationToEvaluation = new Map();
    for (let i = 0; i < iterations.length; i += 1) {
      iterationToEvaluation.set(iterations[i], evaluations[i]);
    }

    const lookupEvaluation = (iteration) => {
      const direct = iterationToEvaluation.get(iteration);
      if (direct != null) return direct;

      let bestIndex = -1;
      let bestDelta = Number.POSITIVE_INFINITY;
      for (let i = 0; i < iterations.length; i += 1) {
        const delta = Math.abs(iterations[i] - iteration);
        if (delta < bestDelta) {
          bestDelta = delta;
          bestIndex = i;
        }
      }
      return bestIndex >= 0 ? evaluations[bestIndex] : null;
    };

    const rawRanges = intervals
      .map((interval) => {
        const startEvaluation = Number(interval?.startEvaluation);
        const endEvaluation = Number(interval?.endEvaluation);
        const startIteration = Number(interval?.startIteration);
        const endIteration = Number(interval?.endIteration);

        let startEval = startEvaluation;
        let endEval = endEvaluation;
        if (!Number.isFinite(startEval) || !Number.isFinite(endEval)) {
          if (!Number.isFinite(startIteration) || !Number.isFinite(endIteration)) return null;
          startEval = lookupEvaluation(startIteration);
          endEval = lookupEvaluation(endIteration);
        }

        if (!Number.isFinite(startEval) || !Number.isFinite(endEval)) return null;
        const start = Math.min(startEval, endEval);
        const end = Math.max(startEval, endEval);
        const phase = typeof interval?.phase === "string" ? interval.phase : "STAGNANT";
        return { start, end, phase };
      })
      .filter(Boolean);

    const sorted = rawRanges.slice().sort((a, b) => a.start - b.start);
    const filled = [];
    for (const range of sorted) {
      const prev = filled[filled.length - 1];
      const start = range.start;
      const end = range.end;

      if (prev && start > prev.end) {
        // Fill the gap with the previous phase so the band stays continuous.
        filled.push({ start: prev.end, end: start, phase: prev.phase });
      }

      if (prev && start < prev.end) {
        // Clip overlaps to avoid color blending.
        if (end <= prev.end) continue;
        filled.push({ ...range, start: prev.end, end });
        continue;
      }

      if (end > start) {
        filled.push(range);
      }
    }

    return filled;
  }, [series.fitnessPhaseIntervals, iterations, evaluations]);

  useEffect(() => {
    const intervals = series.fitnessPhaseIntervals ?? [];
    if (!intervals.length) return;
    // placeholder for future side-effects tied to phase intervals
  }, [series.fitnessPhaseIntervals, run?.problemId, runIndex]);

  const hasAnyData =
    displayKeys.length > 0 &&
    (evaluations.length > 0 || bestFitnessBoxPlot != null);

  if (!hasAnyData) {
    return (
      <div className="run-chart-panel">
        <div className="run-chart-title">{run?.problemId}</div>
        <div>No data to plot.</div>
      </div>
    );
  }

  return (
    <div className="chart-panel">
      <div className="chart-title">
        {run.problemId}
        {instanceName && <span className="chart-instance-name"> — {instanceName}</span>}
      </div>
      {runtimeMs != null && (
        <div className="run-chart-subtitle">
          Runtime: {runtimeMs.toFixed(2)} ms
        </div>
      )}

      <div className="run-chart-inner">
        {effectiveObserver === HYPERCUBE_KEY ? (
          <HypercubePlot run={run} visibleCount={visibleCount} />
        ) : effectiveObserver === TSP_TOUR_KEY ? (
          <TSPVisualization
            run={run}
          />
        ) : isBestFitnessBoxPlot ? (
          <BoxPlotChart
            seriesName="bestFitness"
            boxPlotResponse={bestFitnessBoxPlot}
            searchSpaceId={run?.searchSpaceId}
            xAxisLabel="Evaluation"
            yAxisLabel="bestFitness"
          />
        ) : (
          <LineCharts
            seriesName={effectiveObserver}
            chartPoints={visibleData}
            searchSpaceId={run?.searchSpaceId}
            phaseRanges={phaseRanges}
            xAxisLabel="Evaluation"
            yAxisLabel={effectiveObserver}
            showStats={false}
            onWindowRangeChange={setLineChartWindowRange}
          />
        )}
      </div>

      {displayKeys.length > 1 && (
        <div className="observer-checkboxes">
          {displayKeys.map((key) => {
            // Display friendly names for special keys
            const displayName = key === HYPERCUBE_KEY ? "Hypercube" :  key === TSP_TOUR_KEY ? "TSP Tour" :  key === BEST_FITNESS_BOXPLOT_KEY? "bestFitness boxplot": key;
            return (
              <label key={key} className="observer-checkbox-label">
                <input
                  type="radio"
                  name={`observer-${run.problemId}-${runIndex}`}
                  checked={effectiveObserver === key}
                  onChange={() => handleObserverChange(key)}
                />
                <span>{displayName}</span>
              </label>
            );
          })}
        </div>
      )}

      {effectiveObserver !== HYPERCUBE_KEY && effectiveObserver !== TSP_TOUR_KEY && !isBestFitnessBoxPlot && (
        <div style={{ marginTop: 12 }}>
          <LineChartStatsPanel
            seriesName={effectiveObserver}
            xAxisLabel="Evaluation"
            yAxisLabel={effectiveObserver}
            visiblePoints={statsVisiblePoints}
            windowRange={lineChartWindowRange}
          />
         </div>
       )}
    </div>
  );
}

export default memo(RunChart);

