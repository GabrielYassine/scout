import { useState, useMemo, memo, useCallback, useEffect, useRef } from "react";
import "./RunChart.css";

import HypercubePlot from "./HypercubePlot.jsx";
import TSPVisualization from "./RouteVisualization/RouteVisualization.jsx";
import LineCharts, { LineChartStatsPanel } from "./LineCharts.jsx";
import BoxPlotChart from "./BoxPlotChart.jsx";

const HYPERCUBE_KEY = "__hypercube__";
const TSP_TOUR_KEY = "__tsp-tour__";
const BEST_FITNESS_BOXPLOT_KEY = "__boxplot__:bestFitness";

const SPECIAL_SERIES_KEYS = new Set([
  "hypercubeX",
  "hypercubeY",
  "tspTour",
  "tspCities",
  "pheromoneHeatmap",
  "fitnessPhaseIntervals",
]);

function getRunStatusMeta(run) {
  const rawStatus = String(run?.status ?? "").toUpperCase();

  switch (rawStatus) {
    case "FINISHED":
    case "COMPLETED":
    case "DONE":
      return { label: "Received final package", className: "finished" };

    case "FAILED":
    case "ERROR":
      return { label: "Failed", className: "failed" };

    case "ONGOING":
    case "RUNNING":
    case "IN_PROGRESS":
    default:
      return { label: "Awaiting final package", className: "ongoing" };
  }
}

function getObserverDisplayName(observerKey) {
  switch (observerKey) {
    case HYPERCUBE_KEY:
      return "Hypercube";
    case TSP_TOUR_KEY:
      return "TSP Tour";
    case BEST_FITNESS_BOXPLOT_KEY:
      return "bestFitness boxplot";
    default:
      return observerKey;
  }
}

function getLineSeriesKeys(series) {
  return Object.keys(series).filter((key) => !SPECIAL_SERIES_KEYS.has(key));
}

function buildDisplayKeys({ lineSeriesKeys, hasHypercube, hasTspTour, hasBestFitnessBoxPlot }) {
  const keys = [...lineSeriesKeys];

  if (hasHypercube) keys.push(HYPERCUBE_KEY);
  if (hasTspTour) keys.push(TSP_TOUR_KEY);
  if (hasBestFitnessBoxPlot) keys.push(BEST_FITNESS_BOXPLOT_KEY);

  return keys;
}

function getFallbackObserver(displayKeys, hasTspTour) {
  if (displayKeys.length > 0) return displayKeys[0];
  if (hasTspTour) return TSP_TOUR_KEY;
  return null;
}

function buildPhaseRanges(phaseIntervals, iterations, evaluations) {
  if (!phaseIntervals?.length || !iterations.length || !evaluations.length) {
    return [];
  }

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

  const rawRanges = phaseIntervals
    .map((interval) => {
      const startEvaluation = Number(interval?.startEvaluation);
      const endEvaluation = Number(interval?.endEvaluation);
      const startIteration = Number(interval?.startIteration);
      const endIteration = Number(interval?.endIteration);

      let startEval = startEvaluation;
      let endEval = endEvaluation;

      if (!Number.isFinite(startEval) || !Number.isFinite(endEval)) {
        if (!Number.isFinite(startIteration) || !Number.isFinite(endIteration)) {
          return null;
        }

        startEval = lookupEvaluation(startIteration);
        endEval = lookupEvaluation(endIteration);
      }

      if (!Number.isFinite(startEval) || !Number.isFinite(endEval)) {
        return null;
      }

      return {
        start: Math.min(startEval, endEval),
        end: Math.max(startEval, endEval),
        phase: typeof interval?.phase === "string" ? interval.phase : "STAGNANT",
      };
    })
    .filter(Boolean);

  const sortedRanges = rawRanges.slice().sort((a, b) => a.start - b.start);
  const filledRanges = [];

  for (const range of sortedRanges) {
    const previous = filledRanges[filledRanges.length - 1];

    if (previous && range.start > previous.end) {
      filledRanges.push({
        start: previous.end,
        end: range.start,
        phase: previous.phase,
      });
    }

    if (previous && range.start < previous.end) {
      if (range.end <= previous.end) {
        continue;
      }

      filledRanges.push({
        ...range,
        start: previous.end,
      });
      continue;
    }

    if (range.end > range.start) {
      filledRanges.push(range);
    }
  }

  return filledRanges;
}

function createEmptyPointCache(observerKey) {
  return {
    observerKey,
    lastEvalLen: 0,
    lastYLen: 0,
    points: [],
  };
}

function buildChartPoints({
  effectiveObserver,
  evaluations,
  series,
  isStandardLineSeries,
  pointsCacheRef,
}) {
  if (!isStandardLineSeries) {
    pointsCacheRef.current = createEmptyPointCache(effectiveObserver);
    return [];
  }

  const observerValues = series[effectiveObserver] ?? [];
  const evaluationLength = evaluations.length;
  const valueLength = observerValues.length;
  const sharedLength = Math.min(evaluationLength, valueLength);

  const cache = pointsCacheRef.current;
  const sameObserver = cache.observerKey === effectiveObserver;

  const shouldRebuild =
    !sameObserver ||
    cache.lastEvalLen > evaluationLength ||
    cache.lastYLen > valueLength ||
    cache.points.length > sharedLength;

  if (shouldRebuild) {
    const rebuiltPoints = [];

    for (let i = 0; i < sharedLength; i += 1) {
      const x = Number(evaluations[i]);
      const y = Number(observerValues[i]);

      if (Number.isFinite(x) && Number.isFinite(y)) {
        rebuiltPoints.push([x, y]);
      }
    }

    pointsCacheRef.current = {
      observerKey: effectiveObserver,
      lastEvalLen: evaluationLength,
      lastYLen: valueLength,
      points: rebuiltPoints,
    };

    return rebuiltPoints;
  }

  const startIndex = cache.points.length;

  if (startIndex >= sharedLength) {
    cache.lastEvalLen = evaluationLength;
    cache.lastYLen = valueLength;
    return cache.points;
  }

  const nextPoints = cache.points.slice();

  for (let i = startIndex; i < sharedLength; i += 1) {
    const x = Number(evaluations[i]);
    const y = Number(observerValues[i]);

    if (Number.isFinite(x) && Number.isFinite(y)) {
      nextPoints.push([x, y]);
    }
  }

  pointsCacheRef.current = {
    observerKey: effectiveObserver,
    lastEvalLen: evaluationLength,
    lastYLen: valueLength,
    points: nextPoints,
  };

  return nextPoints;
}

function RunChart({
  run,
  runIndex,
  visibleCount,
  instanceName = null,
  bestFitnessBoxPlot = null,
}) {
  const evaluations = run?.evaluations ?? [];
  const iterations = run?.iterations ?? [];
  const series = run?.series ?? {};
  const runtimeMs = Number.isFinite(run?.runtimeMs) ? run.runtimeMs : null;

  const hasHypercube =
    (series.hypercubeX?.length ?? 0) > 0 &&
    (series.hypercubeY?.length ?? 0) > 0;

  const hasTspTour =
    (series.tspTour?.length ?? 0) > 0 &&
    (series.tspCities?.length ?? 0) > 0;

  const lineSeriesKeys = useMemo(() => getLineSeriesKeys(series), [series]);

  const displayKeys = useMemo(() => {
    return buildDisplayKeys({
      lineSeriesKeys,
      hasHypercube,
      hasTspTour,
      hasBestFitnessBoxPlot: bestFitnessBoxPlot != null,
    });
  }, [lineSeriesKeys, hasHypercube, hasTspTour, bestFitnessBoxPlot]);

  const fallbackObserver = useMemo(() => {
    return getFallbackObserver(displayKeys, hasTspTour);
  }, [displayKeys, hasTspTour]);

  const [selectedObserver, setSelectedObserver] = useState(fallbackObserver);
  const [lineChartWindowRange, setLineChartWindowRange] = useState(null);

  useEffect(() => {
    setSelectedObserver((current) =>
      displayKeys.includes(current) ? current : fallbackObserver
    );
  }, [displayKeys, fallbackObserver]);

  const effectiveObserver = displayKeys.includes(selectedObserver)
    ? selectedObserver
    : fallbackObserver;

  const isHypercube = effectiveObserver === HYPERCUBE_KEY;
  const isTspTour = effectiveObserver === TSP_TOUR_KEY;
  const isBestFitnessBoxPlot = effectiveObserver === BEST_FITNESS_BOXPLOT_KEY;

  const isStandardLineSeries =
    !!effectiveObserver &&
    !isHypercube &&
    !isTspTour &&
    !isBestFitnessBoxPlot &&
    Array.isArray(series[effectiveObserver]);

  const statusMeta = getRunStatusMeta(run);

  const handleObserverChange = useCallback((observerKey) => {
    setSelectedObserver(observerKey);
  }, []);

  const pointsCacheRef = useRef(createEmptyPointCache(null));

  const chartPoints = useMemo(() => {
    return buildChartPoints({
      effectiveObserver,
      evaluations,
      series,
      isStandardLineSeries,
      pointsCacheRef,
    });
  }, [effectiveObserver, evaluations, series, isStandardLineSeries]);

  const visibleChartPoints = useMemo(() => {
    return chartPoints.slice(0, visibleCount);
  }, [chartPoints, visibleCount]);

  const statsVisiblePoints = useMemo(() => {
    if (!lineChartWindowRange) return visibleChartPoints;

    return visibleChartPoints.filter(
      ([x]) => x >= lineChartWindowRange.min && x <= lineChartWindowRange.max
    );
  }, [visibleChartPoints, lineChartWindowRange]);

  const phaseRanges = useMemo(() => {
    return buildPhaseRanges(series.fitnessPhaseIntervals ?? [], iterations, evaluations);
  }, [series.fitnessPhaseIntervals, iterations, evaluations]);

  const hasAnyData =
    displayKeys.length > 0 &&
    (evaluations.length > 0 || bestFitnessBoxPlot != null);

  if (!hasAnyData) {
    return (
      <div className="run-chart-panel">
        <div className="run-chart-header">
          <div className="chart-title-row">
            <div className="chart-title">
              {run?.problemId}
              {instanceName && (
                <span className="chart-instance-name"> — {instanceName}</span>
              )}
            </div>

            <div className={`run-status-indicator ${statusMeta.className}`}>
              <span className="run-status-dot" />
              <span className="run-status-text">{statusMeta.label}</span>
            </div>
          </div>
        </div>

        <div>No data to plot.</div>
      </div>
    );
  }

  return (
    <div className="chart-panel">
      <div className="run-chart-header">
        <div className="chart-title-row">
          <div className="chart-title">
            {run?.problemId}
            {instanceName && (
              <span className="chart-instance-name"> — {instanceName}</span>
            )}
          </div>

          <div className={`run-status-indicator ${statusMeta.className}`}>
            <span className="run-status-text">{statusMeta.label}</span>
            <span className="run-status-dot" />
          </div>
        </div>

        {runtimeMs != null && (
          <div className="run-chart-subtitle">
            Runtime: {runtimeMs.toFixed(2)} ms
          </div>
        )}
      </div>

      <div className="run-chart-inner">
        {isHypercube ? (
          <HypercubePlot run={run} visibleCount={visibleCount} />
        ) : isTspTour ? (
          <TSPVisualization run={run} />
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
            chartPoints={visibleChartPoints}
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
          {displayKeys.map((observerKey) => (
            <label key={observerKey} className="observer-checkbox-label">
              <input
                type="radio"
                name={`observer-${run?.problemId}-${runIndex}`}
                checked={effectiveObserver === observerKey}
                onChange={() => handleObserverChange(observerKey)}
              />
              <span>{getObserverDisplayName(observerKey)}</span>
            </label>
          ))}
        </div>
      )}

      {isStandardLineSeries && (
        <div className="run-chart-stats">
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