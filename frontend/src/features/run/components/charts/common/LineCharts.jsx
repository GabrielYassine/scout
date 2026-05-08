/**
   * Reusable line chart component for run charts.
  * @author s230632
 */

import { useMemo, memo, useState, useRef, useCallback, useEffect } from "react";
import ReactECharts from "echarts-for-react";

import {
  buildLineChartBaseOption,
  buildMarkAreaData,
  buildSeriesPatch,
} from "./lineChartOptions.js";
import {
  getDataXRange,
  rangesEqual,
  resolveZoomRange,
} from "./lineChartZoom.js";

import "@/features/run/styles/LineCharts.css";

// For TSP/VRP fitness series, the framework maximizes negative tour lengths.
// This function identifies those cases so the chart can invert values and labels for display.
function isMinimizationFitnessSeries(seriesName, searchSpaceId) {
  return (
    (seriesName === "fitness" || seriesName === "bestFitness") &&
    (searchSpaceId === "permutation" || searchSpaceId === "route-list")
  );
}

function getDisplaySeriesName(seriesName, searchSpaceId) {
  return isMinimizationFitnessSeries(seriesName, searchSpaceId)
    ? seriesName === "bestFitness" ? "bestTourLength" : "tourLength" : seriesName;
}

function getDisplayChartPoints(chartPoints, seriesName, searchSpaceId) {
  if (!chartPoints?.length) return [];

  if (!isMinimizationFitnessSeries(seriesName, searchSpaceId)) {
    return chartPoints;
  }

  return chartPoints.map(([x, y]) => [x, -y]);
}

function LineCharts({
  chartPoints,
  seriesName,

  rightChartPoints = [],
  rightSeriesName = null,

  xAxisLabel = "Evaluation",
  yAxisLabel = null,
  rightYAxisLabel = null,

  searchSpaceId = null,
  phaseRanges = [],
  enableDataZoom = true,
  onWindowRangeChange = null,
}) {
  const chartInstanceRef = useRef(null);
  const userHasZoomedRef = useRef(false);
  const [windowRange, setWindowRange] = useState(null);

  const displaySeriesName = getDisplaySeriesName(seriesName, searchSpaceId);
  const displayRightSeriesName = rightSeriesName ? getDisplaySeriesName(rightSeriesName, searchSpaceId) : null;

  const resolvedYAxisLabel = yAxisLabel ?? displaySeriesName;
  const resolvedRightYAxisLabel = rightYAxisLabel ?? displayRightSeriesName ?? "";

  const displayChartPoints = useMemo(() => {
    return getDisplayChartPoints(chartPoints, seriesName, searchSpaceId);
  }, [chartPoints, seriesName, searchSpaceId]);

  const displayRightChartPoints = useMemo(() => {
    if (!rightSeriesName) return [];
    return getDisplayChartPoints(rightChartPoints, rightSeriesName, searchSpaceId);
  }, [rightChartPoints, rightSeriesName, searchSpaceId]);

  const markAreaData = useMemo(() => {
    return buildMarkAreaData(phaseRanges);
  }, [phaseRanges]);

  const dataXRange = useMemo(() => {
    return getDataXRange(displayChartPoints);
  }, [displayChartPoints]);

  const baseOption = useMemo(() => {
    return buildLineChartBaseOption({
      leftSeriesName: displaySeriesName,
      rightSeriesName: displayRightSeriesName,
      xAxisLabel,
      leftYAxisLabel: resolvedYAxisLabel,
      rightYAxisLabel: resolvedRightYAxisLabel,
      enableDataZoom,
    });
  }, [
    displaySeriesName,
    displayRightSeriesName,
    xAxisLabel,
    resolvedYAxisLabel,
    resolvedRightYAxisLabel,
    enableDataZoom,
  ]);

  const applySeriesPatch = useCallback(
    (chartInstance) => {
      if (!chartInstance || !seriesName) return;

      const seriesPatch = buildSeriesPatch({
        leftSeriesName: displaySeriesName,
        leftChartPoints: displayChartPoints,
        rightSeriesName: displayRightSeriesName,
        rightChartPoints: displayRightChartPoints,
        markAreaData,
      });

      try {
        chartInstance.setOption(
          { series: seriesPatch },
          {
            notMerge: false,
            lazyUpdate: true,
          }
        );
      } catch {
        // Ignore disposed chart instance races during unmount/re-render.
      }
    },
    [
      seriesName,
      displaySeriesName,
      displayChartPoints,
      displayRightSeriesName,
      displayRightChartPoints,
      markAreaData,
    ]
  );

  // Push new live points into the existing chart without rebuilding the whole option.
  useEffect(() => {
    const chart = chartInstanceRef.current;
    if (!chart) return;

    applySeriesPatch(chart);
  }, [applySeriesPatch]);

  // When switching observer/series, reset zoom and stats window.
  useEffect(() => {
    userHasZoomedRef.current = false;
    setWindowRange(null);
    onWindowRangeChange?.(null);
  }, [seriesName, rightSeriesName, onWindowRangeChange]);

  // While the user has not manually zoomed, keep the active window equal to
  // the full data range. This allows the stats panel to follow live updates.
  useEffect(() => {
    if (!dataXRange) {
      setWindowRange(null);
      onWindowRangeChange?.(null);
      return;
    }

    if (!userHasZoomedRef.current && !rangesEqual(windowRange, dataXRange)) {
      setWindowRange(dataXRange);
      onWindowRangeChange?.(dataXRange);
    }
  }, [dataXRange, windowRange, onWindowRangeChange]);

  const updateRangeFromInstance = useCallback(
    (chartInstance) => {
      if (!chartInstance || !dataXRange) return;

      const optionRef = chartInstance.getOption?.();
      const zoom = optionRef?.dataZoom?.[0] ?? null;
      const nextRange = resolveZoomRange(zoom, dataXRange);

      if (!nextRange) return;

      userHasZoomedRef.current = !rangesEqual(nextRange, dataXRange);

      if (!rangesEqual(windowRange, nextRange)) {
        setWindowRange(nextRange);
        onWindowRangeChange?.(nextRange);
      }
    },
    [dataXRange, windowRange, onWindowRangeChange]
  );

  const handleChartReady = useCallback(
    (chartInstance) => {
      chartInstanceRef.current = chartInstance;
      applySeriesPatch(chartInstance);
      updateRangeFromInstance(chartInstance);
    },
    [applySeriesPatch, updateRangeFromInstance]
  );

  // ECharts emits datazoom when the slider changes. We translate that into
  // numeric x-axis bounds so the stats panel can summarize the selected window.
  useEffect(() => {
    const chartInstance = chartInstanceRef.current;
    if (!chartInstance) return;

    const handler = () => updateRangeFromInstance(chartInstance);

    chartInstance.off?.("datazoom");
    chartInstance.on?.("datazoom", handler);

    return () => {
      chartInstance.off?.("datazoom", handler);
    };
  }, [updateRangeFromInstance]);

  if (!baseOption || !seriesName) {
    return <div>No chart data.</div>;
  }

  return (
    <div className="line-chart-container">
      <ReactECharts
        option={baseOption}
        notMerge={false}
        lazyUpdate={true}
        opts={{ renderer: "canvas" }}
        className="line-chart-echarts"
        style={{ width: "100%", height: "100%" }}
        onChartReady={handleChartReady}
      />
    </div>
  );
}

export default memo(LineCharts);