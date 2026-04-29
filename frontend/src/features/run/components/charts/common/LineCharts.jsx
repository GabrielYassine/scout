/**
 * ECharts wrapper for line-series data.
 * Handles live data patching and reports the current zoom window for statistics.
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

function isMinimizationFitnessSeries(seriesName, searchSpaceId) {
  return (
    (seriesName === "fitness" || seriesName === "bestFitness") &&
    (searchSpaceId === "permutation" || searchSpaceId === "route-list")
  );
}

function LineCharts({
  chartPoints,
  seriesName,
  xAxisLabel = "Evaluation",
  yAxisLabel = null,
  searchSpaceId = null,
  phaseRanges = [],
  enableDataZoom = true,
  onWindowRangeChange = null,
}) {
  const chartInstanceRef = useRef(null);
  const userHasZoomedRef = useRef(false);
  const [windowRange, setWindowRange] = useState(null);

  // TSP and VRP fitness is stored as negative distance internally.
  // For chart display, fitness/bestFitness is shown as positive tour length.
  const shouldInvertFitness = isMinimizationFitnessSeries(seriesName, searchSpaceId);
  const displaySeriesName = shouldInvertFitness ? "tourLength" : seriesName;
  const resolvedYAxisLabel = yAxisLabel ?? displaySeriesName;

  const displayChartPoints = useMemo(() => {
    if (!chartPoints.length) return [];

    return shouldInvertFitness? chartPoints.map(([x, y]) => [x, -y]) : chartPoints;
  }, [chartPoints, shouldInvertFitness]);

  const markAreaData = useMemo(() => {
    return buildMarkAreaData(phaseRanges);
  }, [phaseRanges]);

  const dataXRange = useMemo(() => {
    return getDataXRange(displayChartPoints);
  }, [displayChartPoints]);

  const baseOption = useMemo(() => {
    return buildLineChartBaseOption({
      seriesName,
      displaySeriesName,
      xAxisLabel,
      resolvedYAxisLabel,
      enableDataZoom,
    });
  }, [
    seriesName,
    displaySeriesName,
    xAxisLabel,
    resolvedYAxisLabel,
    enableDataZoom,
  ]);

  const applySeriesPatch = useCallback(
    (chartInstance) => {
      if (!chartInstance || !seriesName) return;

      const seriesPatch = buildSeriesPatch({
        displaySeriesName,
        displayChartPoints,
        markAreaData,
      });

      try {
        chartInstance.setOption(
          { series: [seriesPatch] },
          {
            notMerge: false,
            lazyUpdate: true,
          }
        );
      } catch {
        // Ignore disposed chart instance races during unmount/re-render.
      }
    },
    [seriesName, displaySeriesName, displayChartPoints, markAreaData]
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
  }, [seriesName, onWindowRangeChange]);

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