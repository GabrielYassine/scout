/**
 * ECharts wrapper for line-series data.
 * Handles live data patching and reports the current zoom window for statistics.
 */
import { useMemo, memo, useState, useRef, useCallback, useEffect } from "react";
import ReactECharts from "echarts-for-react";
import LineChartStatsPanel from "./LineChartStatsPanel.jsx";
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

function LineCharts({
  chartPoints,
  seriesName,
  xAxisLabel = "Evaluation",
  yAxisLabel = null,
  searchSpaceId = null,
  phaseRanges = [],
  enableDataZoom = true,
  invertPermutationFitness = true,
  showStats = true,
  onWindowRangeChange = null,
}) {
  const echartsRef = useRef(null);
  const chartInstanceRef = useRef(null);
  const userHasZoomedRef = useRef(false);
  const [windowRange, setWindowRange] = useState(null);

  const isPermutationFitness = useMemo(() => {
    return (
      invertPermutationFitness &&
      (seriesName === "fitness" || seriesName === "bestFitness") &&
      (searchSpaceId === "permutation" || searchSpaceId === "route-list")
    );
  }, [invertPermutationFitness, searchSpaceId, seriesName]);

  const displaySeriesName = isPermutationFitness ? "tourLength" : seriesName;
  const resolvedYAxisLabel = yAxisLabel ?? displaySeriesName;

  const displayChartPoints = useMemo(() => {
    if (!chartPoints?.length) return [];

    return isPermutationFitness ? chartPoints.map(([x, y]) => [x, -y]) : chartPoints;
  }, [chartPoints, isPermutationFitness]);

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
          {
            series: [seriesPatch],
          },
          {
            notMerge: false,
            lazyUpdate: true,
          }
        );
      } catch {
        // ignore disposed instance races
      }
    },
    [seriesName, displaySeriesName, displayChartPoints, markAreaData]
  );

  useEffect(() => {
    const chart = chartInstanceRef.current;
    if (!chart) return;
    applySeriesPatch(chart);
  }, [applySeriesPatch]);

  useEffect(() => {
    userHasZoomedRef.current = false;
    setWindowRange(null);
    onWindowRangeChange?.(null);
  }, [seriesName, onWindowRangeChange]);

  useEffect(() => {
    if (!dataXRange) {
      setWindowRange(null);
      onWindowRangeChange?.(null);
      return;
    }

    if (!userHasZoomedRef.current) {
      if (!windowRange || !rangesEqual(windowRange, dataXRange)) {
        setWindowRange(dataXRange);
        onWindowRangeChange?.(dataXRange);
      }
    }
  }, [dataXRange, windowRange, onWindowRangeChange]);

  const visiblePoints = useMemo(() => {
    if (!windowRange || !displayChartPoints.length) {
      return displayChartPoints;
    }

    return displayChartPoints.filter(
      ([x]) => x >= windowRange.min && x <= windowRange.max
    );
  }, [displayChartPoints, windowRange]);

  const updateRangeFromInstance = useCallback(
    (chartInstance) => {
      if (!chartInstance || !dataXRange) return;

      const optionRef = chartInstance.getOption?.();
      const zoomEntries = optionRef?.dataZoom ?? [];
      const zoom = zoomEntries.find((z) => Number(z?.xAxisIndex ?? 0) === 0) ?? zoomEntries[0] ?? null;

      const nextRange = resolveZoomRange(zoom, dataXRange);
      if (!nextRange) return;

      const zoomedAwayFromFull = !rangesEqual(nextRange, dataXRange);
      userHasZoomedRef.current = zoomedAwayFromFull;

      if (!windowRange || !rangesEqual(windowRange, nextRange)) {
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
      {showStats && (
        <LineChartStatsPanel
          seriesName={seriesName}
          xAxisLabel={xAxisLabel}
          yAxisLabel={yAxisLabel}
          visiblePoints={visiblePoints}
          windowRange={windowRange}
        />
      )}

      <ReactECharts
        ref={echartsRef}
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