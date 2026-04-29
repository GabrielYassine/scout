/**
 * Chart container for a single problem result.
 * Selects between line chart, hypercube, route visualization, and boxplot views
 * depending on the observer data available in the run.
 */
import { useState, useMemo, memo, useCallback, useEffect, useRef } from "react";
import "@/features/run/styles/ChartPanel.css";
import "@/features/run/styles/RunChart.css";

import HypercubePlot from "../hypercube/HypercubePlot.jsx";
import RouteVisualization from "../route/RouteVisualization.jsx";
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
  getInitialObserver,
  getLineSeriesKeys,
  getObserverDisplayName,
  getRunStatusMeta,
  invertFitnessPoints,
  isMinimizationFitnessObserver,
  isSpecialObserver,
} from "./runChartData.js";

function RunChart({
  run,
  runIndex,
  visibleCount,
  instanceName = null,
  bestFitnessBoxPlot = null,
}) {
  const evaluations = run.evaluations ?? [];
  const series = run.series ?? {};
  const runtimeMs = Number.isFinite(run.runtimeMs) ? run.runtimeMs : null;

  const hasHypercube = series.hypercubeX?.length > 0 && series.hypercubeY?.length > 0;
  const hasTspTour = series.tspTour?.length > 0 && series.tspCities?.length > 0;

  // Standard observer series are shown as line charts.
  // Special observer series are handled by dedicated visualizations.
  const lineSeriesKeys = useMemo(() => getLineSeriesKeys(series), [series]);

  const displayKeys = useMemo(() => {
    return buildDisplayKeys({
      lineSeriesKeys,
      hasHypercube,
      hasTspTour,
      hasBestFitnessBoxPlot: bestFitnessBoxPlot !== null,
    });
  }, [lineSeriesKeys, hasHypercube, hasTspTour, bestFitnessBoxPlot]);

  const initialObserver = useMemo(
    () => getInitialObserver(displayKeys),
    [displayKeys]
  );

  const [selectedObserver, setSelectedObserver] = useState(initialObserver);
  const [lineChartWindowRange, setLineChartWindowRange] = useState(null);

  // If live data changes the available observers, keep the selected observer valid.
  useEffect(() => {
    setSelectedObserver((current) =>
      displayKeys.includes(current) ? current : initialObserver
    );
  }, [displayKeys, initialObserver]);

  const activeObserver = displayKeys.includes(selectedObserver) ? selectedObserver : initialObserver;

  const isHypercube = activeObserver === HYPERCUBE_KEY;
  const isTspTour = activeObserver === TSP_TOUR_KEY;
  const isBestFitnessBoxPlot = activeObserver === BEST_FITNESS_BOXPLOT_KEY;

  const isLineSeries =
    activeObserver !== null &&
    !isSpecialObserver(activeObserver) &&
    Array.isArray(series[activeObserver]);

  const statusMeta = getRunStatusMeta(run);

  const handleObserverChange = useCallback((observerKey) => {
    setSelectedObserver(observerKey);
  }, []);

  // Cache point construction so live websocket updates append points instead of
  // rebuilding the full line series every render.
  const pointsCacheRef = useRef(createEmptyPointCache(null));

  const chartPoints = useMemo(() => {
    return buildChartPoints({
      observerKey: activeObserver,
      evaluations,
      series,
      isLineSeries,
      pointsCacheRef,
    });
  }, [activeObserver, evaluations, series, isLineSeries]);

  const visibleChartPoints = useMemo(() => {
    return chartPoints.slice(0, visibleCount);
  }, [chartPoints, visibleCount]);

  // The stats panel should use the same display values as the chart.
  // TSP/VRP fitness is internally negative, so it is inverted for readable stats.
  const statsChartPoints = useMemo(() => {
    if (!isMinimizationFitnessObserver(activeObserver, run.searchSpaceId)) {
      return visibleChartPoints;
    }

    return invertFitnessPoints(visibleChartPoints);
  }, [visibleChartPoints, activeObserver, run.searchSpaceId]);

  useEffect(() => {
    setLineChartWindowRange(null);
  }, [activeObserver]);

  const statsVisiblePoints = useMemo(() => {
    if (!lineChartWindowRange) {
      return statsChartPoints;
    }

    return statsChartPoints.filter(
      ([x]) => x >= lineChartWindowRange.min && x <= lineChartWindowRange.max
    );
  }, [statsChartPoints, lineChartWindowRange]);

  const phaseRanges = useMemo(() => {
    return buildPhaseRanges(series.fitnessPhaseIntervals ?? []);
  }, [series.fitnessPhaseIntervals]);

  const hasAnyData =
    displayKeys.length > 0 &&
    (evaluations.length > 0 || bestFitnessBoxPlot !== null);

  if (!hasAnyData) {
    return (
      <div className="run-chart-panel">
        <RunChartHeader
          problemId={run.problemId}
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
        problemId={run.problemId}
        instanceName={instanceName}
        statusMeta={statusMeta}
        runtimeMs={runtimeMs}
      />

      <div className="run-chart-inner">
        {isHypercube ? (
          <HypercubePlot run={run} visibleCount={visibleCount} />
        ) : isTspTour ? (
          <RouteVisualization run={run} />
        ) : isBestFitnessBoxPlot ? (
          <BoxPlotChart
            seriesName="bestFitness"
            boxPlotResponse={bestFitnessBoxPlot}
            searchSpaceId={run.searchSpaceId}
            xAxisLabel="Evaluation"
            yAxisLabel="bestFitness"
          />
        ) : (
          <LineCharts
            key={`line-${run.problemId}-${runIndex}-${activeObserver}`}
            seriesName={activeObserver}
            chartPoints={visibleChartPoints}
            searchSpaceId={run.searchSpaceId}
            phaseRanges={phaseRanges}
            xAxisLabel="Evaluation"
            yAxisLabel={activeObserver}
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
                name={`observer-${run.problemId}-${runIndex}`}
                checked={activeObserver === observerKey}
                onChange={() => handleObserverChange(observerKey)}
              />
              <span>{getObserverDisplayName(observerKey)}</span>
            </label>
          ))}
        </div>
      )}

      {isLineSeries && (
        <div className="run-chart-stats">
          <LineChartStatsPanel
            seriesName={activeObserver}
            xAxisLabel="Evaluation"
            yAxisLabel={activeObserver}
            visiblePoints={statsVisiblePoints}
            windowRange={lineChartWindowRange}
          />
        </div>
      )}
    </div>
  );
}

export default memo(RunChart);