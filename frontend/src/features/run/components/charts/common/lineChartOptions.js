const PHASE_COLORS = {
  IMPROVING: "rgba(46, 204, 113, 0.18)",
  WORSENING: "rgba(231, 76, 60, 0.18)",
  STAGNANT: "rgba(241, 196, 15, 0.18)",
};

export function buildMarkAreaData(phaseRanges) {
  return (phaseRanges ?? [])
    .map((range) => {
      const start = Number(range?.start);
      const end = Number(range?.end);

      if (!Number.isFinite(start) || !Number.isFinite(end)) return null;

      const color = PHASE_COLORS[range?.phase] ?? PHASE_COLORS.STAGNANT;

      return [
        { xAxis: Math.min(start, end), itemStyle: { color } },
        { xAxis: Math.max(start, end) },
      ];
    })
    .filter(Boolean);
}

export function buildLineChartBaseOption({
  seriesName,
  displaySeriesName,
  xAxisLabel,
  resolvedYAxisLabel,
  enableDataZoom,
}) {
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
}

export function buildSeriesPatch({
  displaySeriesName,
  displayChartPoints,
  markAreaData,
}) {
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

  return seriesPatch;
}