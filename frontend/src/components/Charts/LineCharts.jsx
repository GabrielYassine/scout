import { useMemo, memo } from "react";
import ReactECharts from "echarts-for-react";

function LineCharts({
  chartPoints,
  seriesName,
  xAxisLabel = "Evaluation",
  yAxisLabel = null,
  searchSpaceId = null,
  phaseRanges = [],
  enableDataZoom = true,
  invertPermutationFitness = true,
}) {
  const option = useMemo(() => {
    if (!seriesName || !chartPoints?.length) return null;

    const isPermutationFitness =
      invertPermutationFitness &&
      searchSpaceId === "permutation" &&
      (seriesName === "fitness" || seriesName === "bestFitness");

    const displaySeriesName = isPermutationFitness ? "tourLength" : seriesName;
    const resolvedYAxisLabel = yAxisLabel ?? displaySeriesName;

    const displayChartPoints = isPermutationFitness
      ? chartPoints.map(([x, y]) => [x, -y])
      : chartPoints;

    const phaseColors = {
      IMPROVING: "rgba(46, 204, 113, 0.18)",
      WORSENING: "rgba(231, 76, 60, 0.18)",
      STAGNANT: "rgba(241, 196, 15, 0.18)",
    };

    const markAreaData = (phaseRanges ?? [])
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
      dataZoom: [
        {
          type: "slider",
          xAxisIndex: 0,
          bottom: 16,
          height: 24,
          filterMode: "none",
        },
      ],
      series: [
        {
          name: displaySeriesName,
          type: "line",
          data: displayChartPoints,
          showSymbol: false,
          symbol: "none",
          connectNulls: false,
          lineStyle: {
            width: 2,
          },
          sampling: "lttb",
          progressive: 2000,
          progressiveThreshold: 3000,
          ...(markAreaData.length
            ? {
                markArea: {
                  silent: true,
                  label: { show: false },
                  data: markAreaData,
                },
              }
            : {}),
        },
      ],
    };
  }, [
    chartPoints,
    seriesName,
    xAxisLabel,
    yAxisLabel,
    searchSpaceId,
    phaseRanges,
    invertPermutationFitness,
  ]);

  if (!option) {
    return <div>No chart data.</div>;
  }

  return (
    <ReactECharts
      option={option}
      notMerge={true}
      lazyUpdate={true}
      opts={{ renderer: "canvas" }}
      style={{ width: "100%", height: "100%" }}
    />
  );
}

export default memo(LineCharts);