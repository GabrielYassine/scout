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
// Maps run status to display label and CSS class for styling
function getRunStatusMeta(run) {
  const rawStatus = String(run?.status ?? "").toUpperCase();

  if (rawStatus === "FINISHED") {
    return { label: "Finished", className: "finished" };
  }

  if (rawStatus === "FAILED") {
    return { label: "Failed", className: "failed" };
  }

  return { label: "Running", className: "ongoing" };
}
// Maps internal observer keys to user-friendly display names
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
// Extracts the keys of standard line series from the run's series data, excluding special keys used for other visualizations
function getLineSeriesKeys(series) {
  return Object.keys(series).filter((key) => !SPECIAL_SERIES_KEYS.has(key));
}
// Builds the list of observer keys that can be displayed based on the available data in the run's series.
function buildDisplayKeys({ lineSeriesKeys, hasHypercube, hasTspTour, hasBestFitnessBoxPlot }) {
  const keys = [...lineSeriesKeys];

  if (hasHypercube) keys.push(HYPERCUBE_KEY);
  if (hasTspTour) keys.push(TSP_TOUR_KEY);
  if (hasBestFitnessBoxPlot) keys.push(BEST_FITNESS_BOXPLOT_KEY);

  return keys;
}
// Determines the fallback observer key to use when the currently selected observer is not available.
function getFallbackObserver(displayKeys, hasTspTour) {
  if (displayKeys.length > 0) return displayKeys[0];
  return null;
}
// Normalizes and sorts phase intervals for visualization, ensuring valid numeric values and consistent ordering.
function normalizePhaseRanges(phaseIntervals) {
  return (phaseIntervals ?? [])
    .map((interval) => {
      const startEval = Number(interval?.startEvaluation);
      const endEval = Number(interval?.endEvaluation);

      if (!Number.isFinite(startEval) || !Number.isFinite(endEval)) {
        return null;
      }

      return {
        start: Math.min(startEval, endEval),
        end: Math.max(startEval, endEval),
        // Ensure phase is a valid string, defaulting to "STAGNANT" if not provided or invalid
        phase: typeof interval?.phase === "string" ? interval.phase : "STAGNANT",
      };
    })
    .filter(Boolean)
    .sort((a, b) => a.start - b.start);
}
// Resolves gaps and overlaps in the sorted phase intervals to create a continuous timeline for visualization.
function resolveRangeGapsAndOverlaps(sortedRanges) {
  const resolvedRanges = [];

  for (const range of sortedRanges) {
    const previous = resolvedRanges[resolvedRanges.length - 1];
    // If there's a gap between the previous range and the current range, fill it with a new range using the previous phase
    if (previous && range.start > previous.end) {
      resolvedRanges.push({
        start: previous.end,
        end: range.start,
        phase: previous.phase,
      });
    }
    // If there's an overlap between the previous range and the current range, adjust the current range to start where the previous one ended
    if (previous && range.start < previous.end) {
      if (range.end <= previous.end) {
        continue;
      }
      resolvedRanges.push({
        ...range,
        start: previous.end,
      });
      continue;
    }

    if (range.end > range.start) {
      resolvedRanges.push(range);
    }
  }

  return resolvedRanges;
}

function buildPhaseRanges(phaseIntervals) {
  if (!phaseIntervals?.length) {
    return [];
  }

  const sortedRanges = normalizePhaseRanges(phaseIntervals);
  return resolveRangeGapsAndOverlaps(sortedRanges);
}

// Creates an empty point cache object for a given observer key, initializing lengths and points array.
function createEmptyPointCache(observerKey) {
  return {
    observerKey,
    lastEvalLen: 0,
    lastYLen: 0,
    points: [],
  };
}
// Builds the chart points for a given observer.
function buildChartPoints({
  effectiveObserver,
  evaluations,
  series,
  isStandardLineSeries,
  pointsCacheRef,
}) {
 // If the effective observer is not a standard line series, reset the points cache and return an empty array
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
  // If the observer has changed or if the lengths of evaluations or observer values have decreased,
  // we need to rebuild the points from scratch to ensure consistency.
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
  //else if the observer is the same and lengths have not decreased, we can append new points to the existing cache without rebuilding everything.
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

  const statsChartPoints = useMemo(() => {
    const isMinimizationFitness =
      (effectiveObserver === "fitness" || effectiveObserver === "bestFitness") &&
      (run?.searchSpaceId === "permutation" || run?.searchSpaceId === "route-list");

    if (!isMinimizationFitness) return visibleChartPoints;

    return visibleChartPoints.map(([x, y]) => [x, -y]);
  }, [visibleChartPoints, effectiveObserver, run?.searchSpaceId]);

  useEffect(() => {
    setLineChartWindowRange(null);
  }, [effectiveObserver]);

  const statsVisiblePoints = useMemo(() => {
    if (!lineChartWindowRange) return statsChartPoints;

    return statsChartPoints.filter(
      ([x]) => x >= lineChartWindowRange.min && x <= lineChartWindowRange.max
    );
  }, [statsChartPoints, lineChartWindowRange]);

  const phaseRanges = useMemo(() => {
    return buildPhaseRanges(series.fitnessPhaseIntervals ?? []);
  }, [series.fitnessPhaseIntervals]);

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
        <div className="run-chart-subtitle">
          Runtime: {runtimeMs != null ? `${runtimeMs.toFixed(2)} ms` : "Running..."}
        </div>
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
            key={`line-${run.problemId}-${runIndex}-${effectiveObserver}`}
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