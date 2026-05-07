/**
  *
  * @author s235257 & s230632
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
import RunChartObserverControls from "../../controls/RunChartObserverControls.jsx";
import { useRememberedObserver } from "@/features/run/hooks/useRememberedObserver.js";

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

  const hasHypercube =
    series.hypercubeX?.length > 0 && series.hypercubeY?.length > 0;
  const hasTspTour =
    series.tspTour?.length > 0 && series.tspCities?.length > 0;

  const lineSeriesKeys = useMemo(() => getLineSeriesKeys(series), [series]);

  const displayKeys = useMemo(() => {
    return buildDisplayKeys({
      lineSeriesKeys,
      hasHypercube,
      hasTspTour,
      hasBestFitnessBoxPlot: bestFitnessBoxPlot !== null,
    });
  }, [lineSeriesKeys, hasHypercube, hasTspTour, bestFitnessBoxPlot]);

  const specialDisplayKeys = useMemo(() => {
    return displayKeys.filter(isSpecialObserver);
  }, [displayKeys]);

  const initialObserver = useMemo(
    () => getInitialObserver(displayKeys),
    [displayKeys]
  );

  const [selectedObserver, setSelectedObserver] = useRememberedObserver({problemId: run.problemId, displayKeys, initialObserver,});
  const [rightObserver, setRightObserver] = useState("");
  const [lineChartWindowRange, setLineChartWindowRange] = useState(null);

  const activeObserver = displayKeys.includes(selectedObserver)
    ? selectedObserver
    : initialObserver;

  const isHypercube = activeObserver === HYPERCUBE_KEY;
  const isTspTour = activeObserver === TSP_TOUR_KEY;
  const isBestFitnessBoxPlot = activeObserver === BEST_FITNESS_BOXPLOT_KEY;

  const isLineSeries =
    activeObserver !== null &&
    !isSpecialObserver(activeObserver) &&
    Array.isArray(series[activeObserver]);

  const rightLineSeries =
    isLineSeries &&
    rightObserver &&
    rightObserver !== activeObserver &&
    lineSeriesKeys.includes(rightObserver) &&
    Array.isArray(series[rightObserver])
      ? rightObserver
      : null;

  const statusMeta = getRunStatusMeta(run);

  const handleLeftAxisChange = useCallback((observerKey) => {
    setSelectedObserver(observerKey);
  }, []);

  const handleRightAxisChange = useCallback((observerKey) => {
    setRightObserver(observerKey);
  }, []);

  const handleSpecialObserverChange = useCallback((observerKey) => {
    setSelectedObserver(observerKey);
    setRightObserver("");
  }, []);

  const pointsCacheRef = useRef(createEmptyPointCache(null));
  const rightPointsCacheRef = useRef(createEmptyPointCache(null));

  const chartPoints = useMemo(() => {
    return buildChartPoints({
      observerKey: activeObserver,
      evaluations,
      series,
      isLineSeries,
      pointsCacheRef,
    });
  }, [activeObserver, evaluations, series, isLineSeries]);

  const rightChartPoints = useMemo(() => {
    return buildChartPoints({
      observerKey: rightLineSeries,
      evaluations,
      series,
      isLineSeries: Boolean(rightLineSeries),
      pointsCacheRef: rightPointsCacheRef,
    });
  }, [rightLineSeries, evaluations, series]);

  const visibleChartPoints = useMemo(() => {
    return chartPoints.slice(0, visibleCount);
  }, [chartPoints, visibleCount]);

  const visibleRightChartPoints = useMemo(() => {
    return rightChartPoints.slice(0, visibleCount);
  }, [rightChartPoints, visibleCount]);

  const statsChartPoints = useMemo(() => {
    if (!isMinimizationFitnessObserver(activeObserver, run.searchSpaceId)) {
      return visibleChartPoints;
    }

    return invertFitnessPoints(visibleChartPoints);
  }, [visibleChartPoints, activeObserver, run.searchSpaceId]);

  useEffect(() => {
    setLineChartWindowRange(null);
  }, [activeObserver, rightLineSeries]);

  useEffect(() => {
    if (rightObserver && rightObserver === activeObserver) {
      setRightObserver("");
    }
  }, [activeObserver, rightObserver]);

  useEffect(() => {
    if (rightObserver && !lineSeriesKeys.includes(rightObserver)) {
      setRightObserver("");
    }
  }, [rightObserver, lineSeriesKeys]);

  useEffect(() => {
    if (!isLineSeries && rightObserver) {
      setRightObserver("");
    }
  }, [isLineSeries, rightObserver]);

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
            key={`line-${run.problemId}-${runIndex}-${activeObserver}-${
              rightLineSeries ?? "none"
            }`}
            seriesName={activeObserver}
            chartPoints={visibleChartPoints}
            rightSeriesName={rightLineSeries}
            rightChartPoints={visibleRightChartPoints}
            searchSpaceId={run.searchSpaceId}
            phaseRanges={phaseRanges}
            xAxisLabel="Evaluation"
            yAxisLabel={activeObserver}
            rightYAxisLabel={rightLineSeries}
            onWindowRangeChange={setLineChartWindowRange}
          />
        )}
      </div>

      <RunChartObserverControls
        problemId={run.problemId}
        runIndex={runIndex}
        lineSeriesKeys={lineSeriesKeys}
        specialDisplayKeys={specialDisplayKeys}
        activeObserver={activeObserver}
        rightLineSeries={rightLineSeries}
        isLineSeries={isLineSeries}
        onLeftAxisChange={handleLeftAxisChange}
        onRightAxisChange={handleRightAxisChange}
        onSpecialObserverChange={handleSpecialObserverChange}
      />

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