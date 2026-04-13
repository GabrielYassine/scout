import { useMemo, memo, useState, useRef, useCallback, useEffect } from "react";
import ReactECharts from "echarts-for-react";

const computePercentile = (sorted, p) => {
  if (!sorted.length) return 0;
  if (sorted.length === 1) return sorted[0];
  const index = (p / 100) * (sorted.length - 1);
  const lower = Math.floor(index);
  const upper = Math.ceil(index);
  if (lower === upper) return sorted[lower];
  const fraction = index - lower;
  return sorted[lower] + fraction * (sorted[upper] - sorted[lower]);
};

const computeLocalStats = (points) => {
  if (!points?.length) return null;
  const ys = points.map(([, y]) => Number(y)).filter(Number.isFinite);
  const xs = points.map(([x]) => Number(x)).filter(Number.isFinite);
  if (!ys.length || !xs.length) return null;

  const sorted = [...ys].sort((a, b) => a - b);
  const count = ys.length;
  const mean = ys.reduce((sum, v) => sum + v, 0) / count;
  const variance = ys.reduce((sum, v) => sum + (v - mean) ** 2, 0) / count;
  const stdDev = Math.sqrt(variance);
  const min = sorted[0];
  const max = sorted[sorted.length - 1];
  const median = computePercentile(sorted, 50);
  const q1 = computePercentile(sorted, 25);
  const q3 = computePercentile(sorted, 75);
  const iqr = q3 - q1;

  const xMean = xs.reduce((sum, v) => sum + v, 0) / count;
  const covariance = points.reduce((sum, [x, y]) => {
    const nx = Number(x);
    const ny = Number(y);
    return sum + (nx - xMean) * (ny - mean);
  }, 0) / count;
  const xVariance = xs.reduce((sum, v) => sum + (v - xMean) ** 2, 0) / count;
  const slope = xVariance === 0 ? 0 : covariance / xVariance;
  const trend = slope > 0.0001 ? "up" : slope < -0.0001 ? "down" : "flat";

  return { count, min, max, mean, stdDev, median, q1, q3, iqr, slope, trend };
};

export function LineChartStatsPanel({
  seriesName,
  xAxisLabel = "Evaluation",
  yAxisLabel = null,
  visiblePoints,
  windowRange,
}) {
  const [statsLoading, setStatsLoading] = useState(false);
  const [statsError, setStatsError] = useState("");
  const [windowStats, setWindowStats] = useState(null);

  const computeAndFetchStats = useCallback(async () => {
    const points = visiblePoints;
    if (!points?.length) {
      setStatsError("No points in the current window.");
      setWindowStats(null);
      return;
    }

    const payload = {
      seriesName,
      xAxisLabel,
      yAxisLabel: yAxisLabel ?? seriesName,
      xMin: windowRange?.min ?? points[0][0],
      xMax: windowRange?.max ?? points[points.length - 1][0],
      points,
    };

    setStatsLoading(true);
    setStatsError("");

    try {
      const res = await fetch("/api/stats/series-window", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        let message = `Stats request failed with status ${res.status}`;
        try {
          const data = await res.json();
          if (data?.message) message = data.message;
        } catch {
          // ignore parse failure
        }
        throw new Error(message);
      }

      const data = await res.json();
      setWindowStats(data);
    } catch (err) {
      const fallback = computeLocalStats(points);
      setWindowStats(fallback);
      setStatsError(err?.message ?? "Failed to fetch stats; showing local summary instead.");
    } finally {
      setStatsLoading(false);
    }
  }, [seriesName, xAxisLabel, yAxisLabel, visiblePoints, windowRange]);

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
      <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
        <button type="button" onClick={computeAndFetchStats} disabled={statsLoading || !(visiblePoints?.length > 0)}>
          {statsLoading ? "Computing..." : "Compute stats for zoom window"}
        </button>
        {windowRange && (
          <span style={{ fontSize: 12, color: "#666" }}>
            Window: {windowRange.min} → {windowRange.max} ({visiblePoints?.length ?? 0} points)
          </span>
        )}
      </div>

      {statsError && (
        <div style={{ fontSize: 12, color: "#b45309" }}>{statsError}</div>
      )}

      {windowStats && (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(4, minmax(0, 1fr))", gap: 8, fontSize: 12 }}>
          <div><strong>Count</strong><br />{windowStats.count ?? windowStats?.count ?? "-"}</div>
          <div><strong>Mean</strong><br />{Number(windowStats.mean ?? windowStats?.mean ?? 0).toFixed(4)}</div>
          <div><strong>Median</strong><br />{Number(windowStats.median ?? 0).toFixed(4)}</div>
          <div><strong>Std Dev</strong><br />{Number(windowStats.stdDev ?? windowStats.stddev ?? 0).toFixed(4)}</div>
          <div><strong>Min / Max</strong><br />{Number(windowStats.min ?? 0).toFixed(4)} / {Number(windowStats.max ?? 0).toFixed(4)}</div>
          <div><strong>Q1 / Q3</strong><br />{Number(windowStats.q1 ?? 0).toFixed(4)} / {Number(windowStats.q3 ?? 0).toFixed(4)}</div>
          <div><strong>IQR</strong><br />{Number(windowStats.iqr ?? 0).toFixed(4)}</div>
          <div><strong>Trend</strong><br />{windowStats.trend ?? "-"} ({Number(windowStats.slope ?? 0).toFixed(6)})</div>
        </div>
      )}
    </div>
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
  invertPermutationFitness = true,
  showStats = true,
  onWindowRangeChange = null,
}) {
  const echartsRef = useRef(null);
  const chartInstanceRef = useRef(null);
  const [windowRange, setWindowRange] = useState(null);

  const isPermutationFitness = useMemo(() => {
    return (
      invertPermutationFitness &&
      searchSpaceId === "permutation" &&
      (seriesName === "fitness" || seriesName === "bestFitness")
    );
  }, [invertPermutationFitness, searchSpaceId, seriesName]);

  const displaySeriesName = isPermutationFitness ? "tourLength" : seriesName;
  const resolvedYAxisLabel = yAxisLabel ?? displaySeriesName;

  const displayChartPoints = useMemo(() => {
    if (!chartPoints?.length) return [];
    return isPermutationFitness ? chartPoints.map(([x, y]) => [x, -y]) : chartPoints;
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

  // Fast-path x-range for ordered series.
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
    return { min: Math.min(...xs), max: Math.max(...xs) };
  }, [displayChartPoints]);

  // Stable base option: keep structure stable so ECharts can diff efficiently.
  // Data and markArea are applied via setOption in effects.
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
              filterMode: "filter",
            },
            {
              type: "inside",
              xAxisIndex: 0,
              filterMode: "filter",
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
  }, [displaySeriesName, enableDataZoom, resolvedYAxisLabel, seriesName, xAxisLabel]);

  const applySeriesPatch = useCallback(
    (chartInstance) => {
      if (!chartInstance) return;
      if (!seriesName) return;

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

  // Push live data + markArea updates into the existing chart instance.
  useEffect(() => {
    const chart = chartInstanceRef.current;
    if (!chart) return;
    applySeriesPatch(chart);
  }, [applySeriesPatch]);

  // Keep windowRange in sync when new points arrive.
  useEffect(() => {
    if (!dataXRange) return;
    if (!windowRange) {
      setWindowRange(dataXRange);
      onWindowRangeChange?.(dataXRange);
      return;
    }
    const next = {
      min: Math.min(windowRange.min, dataXRange.min),
      max: Math.max(windowRange.max, dataXRange.max),
    };
    if (next.min !== windowRange.min || next.max !== windowRange.max) {
      setWindowRange(next);
      onWindowRangeChange?.(next);
    }
  }, [dataXRange, windowRange, onWindowRangeChange]);

  const visiblePoints = useMemo(() => {
    if (!windowRange || !displayChartPoints.length) return displayChartPoints;
    return displayChartPoints.filter(([x]) => x >= windowRange.min && x <= windowRange.max);
  }, [displayChartPoints, windowRange]);

  const updateRangeFromInstance = useCallback(
    (chartInstance) => {
      if (!chartInstance) return;
      const optionRef = chartInstance.getOption?.();
      const zoom = optionRef?.dataZoom?.[0];
      const start = Number(zoom?.startValue ?? zoom?.start);
      const end = Number(zoom?.endValue ?? zoom?.end);
      if (Number.isFinite(start) && Number.isFinite(end)) {
        const range = { min: Math.min(start, end), max: Math.max(start, end) };
        setWindowRange(range);
        onWindowRangeChange?.(range);
        return;
      }

      if (dataXRange) {
        setWindowRange(dataXRange);
        onWindowRangeChange?.(dataXRange);
      }
    },
    [dataXRange, onWindowRangeChange]
  );

  const handleChartReady = useCallback(
    (chartInstance) => {
      chartInstanceRef.current = chartInstance;

      // Apply current series data immediately to avoid a hydration race
      // where the effect runs before onChartReady and the chart stays blank.
      applySeriesPatch(chartInstance);

      updateRangeFromInstance(chartInstance);
      chartInstance.off?.("datazoom");
      chartInstance.on?.("datazoom", () => updateRangeFromInstance(chartInstance));
    },
    [applySeriesPatch, updateRangeFromInstance]
  );

  if (!baseOption || !seriesName) {
    return <div>No chart data.</div>;
  }

  return (
    <div style={{ width: "100%", height: "100%", display: "flex", flexDirection: "column", gap: 8 }}>
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
        style={{ width: "100%", height: "100%" }}
        onChartReady={handleChartReady}
      />
    </div>
  );
}

export default memo(LineCharts);

