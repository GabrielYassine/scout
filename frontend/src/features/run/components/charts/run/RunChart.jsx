/**
 * Chart container for a single problem result.
 * Selects between line chart, hypercube, route visualization, and boxplot views
 * depending on the observer data available in the run.
 */
import { useState, useMemo, memo, useCallback, useEffect, useRef } from "react";
import "@/features/run/styles/ChartPanel.css";
import "@/features/run/styles/RunChart.css";

import HypercubePlot from "../hypercube/HypercubePlot.jsx";
import TSPVisualization from "../route/RouteVisualization.jsx";
import LineCharts from "../common/LineCharts.jsx";
import LineChartStatsPanel from "../common/LineChartStatsPanel.jsx";
import BoxPlotChart from "../common/BoxPlotChart.jsx";
import RunChartHeader from "./RunChartHeader.jsx";
import {
  BEST_FITNESS_BOXPLOT_KEY,
  HYPERCUBE_KEY,
  TSP_TOUR_KEY,
  buildChartPoints,
  buildDisplayKeys,
  buildPhaseRanges,
  createEmptyPointCache,
  getFallbackObserver,
  getLineSeriesKeys,
  getObserverDisplayName,
  getRunStatusMeta,
} from "./runChartData.js";

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

  const hasTspTour =(series.tspTour?.length ?? 0) > 0 && (series.tspCities?.length ?? 0) > 0;

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
    return getFallbackObserver(displayKeys);
  }, [displayKeys]);

  const [selectedObserver, setSelectedObserver] = useState(fallbackObserver);
  const [lineChartWindowRange, setLineChartWindowRange] = useState(null);

  useEffect(() => {
    setSelectedObserver((current) =>
      displayKeys.includes(current) ? current : fallbackObserver
    );
  }, [displayKeys, fallbackObserver]);

  const effectiveObserver = displayKeys.includes(selectedObserver) ? selectedObserver : fallbackObserver;

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
        <RunChartHeader
          problemId={run?.problemId}
          instanceName={instanceName}
          statusMeta={statusMeta}
          showRuntime={false}
          dotFirst
        />

        <div>No data to plot.</div>
      </div>
    );
  }

  return (
    <div className="chart-panel">
      <RunChartHeader
        problemId={run?.problemId}
        instanceName={instanceName}
        statusMeta={statusMeta}
        runtimeMs={runtimeMs}
      />

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