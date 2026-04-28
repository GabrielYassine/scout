import { useMemo, memo, useState, useRef, useCallback, useEffect } from "react";
import ReactECharts from "echarts-for-react";
import LineChartStatsPanel from "./LineChartStatsPanel.jsx";
import "./LineCharts.css";

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
    return isPermutationFitness
      ? chartPoints.map(([x, y]) => [x, -y])
      : chartPoints;
  }, [chartPoints, isPermutationFitness]);

  const phaseColors = useMemo(
    () => ({
      IMPROVING: "rgba(46, 204, 113, 0.18)",
      WORSENING: "rgba(231, 76, 60, 0.18)",
      STAGNANT: "rgba(241, 196, 15, 0.18)",
    }),
    []
  );

  const markAreaData = useMemo(() => {
    return (phaseRanges ?? [])
      .map((range) => {
        const start = Number(range?.start);
        const end = Number(range?.end);

        if (!Number.isFinite(start) || !Number.isFinite(end)) return null;

        const color = phaseColors[range?.phase] ?? phaseColors.STAGNANT;

        return [
          { xAxis: Math.min(start, end), itemStyle: { color } },
          { xAxis: Math.max(start, end) },
        ];
      })
      .filter(Boolean);
  }, [phaseRanges, phaseColors]);

  const dataXRange = useMemo(() => {
    if (!displayChartPoints.length) return null;

    const first = displayChartPoints[0]?.[0];
    const last = displayChartPoints[displayChartPoints.length - 1]?.[0];
    const min = Number(first);
    const max = Number(last);

    if (Number.isFinite(min) && Number.isFinite(max)) {
      return { min: Math.min(min, max), max: Math.max(min, max) };
    }

    const xs = displayChartPoints.map(([x]) => Number(x)).filter(Number.isFinite);
    if (!xs.length) return null;

    return {
      min: Math.min(...xs),
      max: Math.max(...xs),
    };
  }, [displayChartPoints]);

  const rangesEqual = useCallback((a, b) => {
    if (!a || !b) return false;
    return Math.abs(a.min - b.min) < 1e-9 && Math.abs(a.max - b.max) < 1e-9;
  }, []);

  const clampRange = useCallback((range, fullRange) => {
    if (!range || !fullRange) return fullRange ?? null;

    return {
      min: Math.max(fullRange.min, Math.min(range.min, fullRange.max)),
      max: Math.max(fullRange.min, Math.min(range.max, fullRange.max)),
    };
  }, []);

  const resolveZoomRange = useCallback(
    (zoom, fullRange) => {
      if (!fullRange) return null;
      if (!zoom) return fullRange;

      const startValue = Number(zoom.startValue);
      const endValue = Number(zoom.endValue);

      if (Number.isFinite(startValue) && Number.isFinite(endValue)) {
        return clampRange(
          {
            min: Math.min(startValue, endValue),
            max: Math.max(startValue, endValue),
          },
          fullRange
        );
      }

      const startPercent = Number(zoom.start);
      const endPercent = Number(zoom.end);

      if (Number.isFinite(startPercent) && Number.isFinite(endPercent)) {
        const startPct = Math.min(startPercent, endPercent) / 100;
        const endPct = Math.max(startPercent, endPercent) / 100;
        const span = fullRange.max - fullRange.min;

        return clampRange(
          {
            min: fullRange.min + startPct * span,
            max: fullRange.min + endPct * span,
          },
          fullRange
        );
      }

      return fullRange;
    },
    [clampRange]
  );

  const baseOption = useMemo(() => {
    if (!seriesName) return null;

    return {
      animation: false,
      grid: {
        top: 24,
        right: 24,
        bottom: 70,
        left: 64,
        containLabel: true,
      },
      tooltip: {
        trigger: "axis",
        axisPointer: { type: "cross" },
        formatter: (params) => {
          const first = Array.isArray(params) ? params[0] : params;
          const value = first?.value ?? [];
          const x = value[0] ?? "-";
          const y = value[1] ?? "-";
          return `${xAxisLabel}: ${x}<br/>${resolvedYAxisLabel}: ${y}`;
        },
      },
      xAxis: {
        type: "value",
        name: xAxisLabel,
        nameLocation: "middle",
        nameGap: 30,
        min: "dataMin",
        max: "dataMax",
      },
      yAxis: {
        type: "value",
        name: resolvedYAxisLabel,
        nameLocation: "middle",
        nameGap: 45,
        scale: true,
      },
      dataZoom: enableDataZoom
        ? [
            {
              type: "slider",
              xAxisIndex: 0,
              bottom: 16,
              height: 24,
              filterMode: "none",
            },
          ]
        : [],
      series: [
        {
          name: displaySeriesName,
          type: "line",
          data: [],
          showSymbol: false,
          symbol: "none",
          connectNulls: false,
          lineStyle: {
            width: 2,
          },
          sampling: "lttb",
          progressive: 2000,
          progressiveThreshold: 3000,
        },
      ],
    };
  }, [
    displaySeriesName,
    enableDataZoom,
    resolvedYAxisLabel,
    seriesName,
    xAxisLabel,
  ]);

  const applySeriesPatch = useCallback(
    (chartInstance) => {
      if (!chartInstance || !seriesName) return;

      const seriesPatch = {
        name: displaySeriesName,
        data: displayChartPoints,
      };

      if (markAreaData.length) {
        seriesPatch.markArea = {
          silent: true,
          label: { show: false },
          data: markAreaData,
        };
      } else {
        seriesPatch.markArea = null;
      }

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
  }, [dataXRange, windowRange, rangesEqual, onWindowRangeChange]);

  const visiblePoints = useMemo(() => {
    if (!windowRange || !displayChartPoints.length) return displayChartPoints;

    return displayChartPoints.filter(
      ([x]) => x >= windowRange.min && x <= windowRange.max
    );
  }, [displayChartPoints, windowRange]);

  const updateRangeFromInstance = useCallback(
    (chartInstance) => {
      if (!chartInstance || !dataXRange) return;

      const optionRef = chartInstance.getOption?.();
      const zoomEntries = optionRef?.dataZoom ?? [];
      const zoom =zoomEntries.find((z) => Number(z?.xAxisIndex ?? 0) === 0) ?? zoomEntries[0] ?? null;

      const nextRange = resolveZoomRange(zoom, dataXRange);
      if (!nextRange) return;

      const zoomedAwayFromFull = !rangesEqual(nextRange, dataXRange);
      userHasZoomedRef.current = zoomedAwayFromFull;

      if (!windowRange || !rangesEqual(windowRange, nextRange)) {
        setWindowRange(nextRange);
        onWindowRangeChange?.(nextRange);
      }
    },
    [dataXRange, windowRange, resolveZoomRange, rangesEqual, onWindowRangeChange]
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