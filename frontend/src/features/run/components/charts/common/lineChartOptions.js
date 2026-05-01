/**
 * Pure builders for ECharts line-chart options and series patches.
 * Kept separate from the React component so chart configuration is easier to read.
 */

const LEFT_AXIS_COLOR = "#1d4ed8";
const RIGHT_AXIS_COLOR = "#06b6d4";

const PHASE_COLORS = {
  IMPROVING: "rgba(46, 204, 113, 0.18)",
  WORSENING: "rgba(231, 76, 60, 0.18)",
  STAGNANT: "rgba(241, 196, 15, 0.18)",
};

/**
 * Converts phase intervals into ECharts markArea ranges.
 * These are the shaded background regions used to show improving, worsening,
 * or stagnant phases on top of a line chart.
 */
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

function getAxisColor(yAxisIndex) {
  return yAxisIndex === 1 ? RIGHT_AXIS_COLOR : LEFT_AXIS_COLOR;
}

function buildYAxis({ name, position, color }) {
  return {
    type: "value",
    name,
    position,
    nameLocation: "middle",
    nameGap: position === "right" ? 48 : 45,
    scale: true,
    axisLine: {
      show: true,
      lineStyle: {
        color,
      },
    },
    axisLabel: {
      color,
    },
    nameTextStyle: {
      color,
    },
  };
}

function buildEmptyLineSeries({ name, yAxisIndex }) {
  const color = getAxisColor(yAxisIndex);

  return {
    name,
    type: "line",
    yAxisIndex,
    data: [],
    showSymbol: false,
    symbol: "none",
    connectNulls: false,
    lineStyle: {
      width: 2,
      color,
    },
    itemStyle: {
      color,
    },
    sampling: "lttb",
    progressive: 2000,
    progressiveThreshold: 3000,
  };
}

/**
 * Builds the stable base ECharts option.
 * Live data is intentionally not inserted here; it is patched separately so
 * the chart can update efficiently during websocket streaming.
 */
export function buildLineChartBaseOption({
  leftSeriesName,
  rightSeriesName = null,
  xAxisLabel,
  leftYAxisLabel,
  rightYAxisLabel = null,
  enableDataZoom,
}) {
  if (!leftSeriesName) return null;

  const hasRightAxis = Boolean(rightSeriesName);

  return {
    animation: false,
    grid: {
      top: 24,
      right: hasRightAxis ? 76 : 24,
      bottom: 70,
      left: 64,
      containLabel: true,
    },
    legend: {
      top: 0,
      type: "scroll",
    },
    tooltip: {
      trigger: "axis",
      axisPointer: { type: "cross" },
      formatter: (params) => {
        const rows = Array.isArray(params) ? params : [params];
        const first = rows[0];
        const x = first?.value?.[0] ?? "-";

        const seriesRows = rows
          .map((item) => {
            const name = item?.seriesName ?? "-";
            const y = item?.value?.[1] ?? "-";
            return `${item.marker ?? ""}${name}: ${y}`;
          })
          .join("<br/>");

        return `${xAxisLabel}: ${x}<br/>${seriesRows}`;
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
    yAxis: hasRightAxis
      ? [
          buildYAxis({
            name: leftYAxisLabel,
            position: "left",
            color: LEFT_AXIS_COLOR,
          }),
          buildYAxis({
            name: rightYAxisLabel,
            position: "right",
            color: RIGHT_AXIS_COLOR,
          }),
        ]
      : buildYAxis({
          name: leftYAxisLabel,
          position: "left",
          color: LEFT_AXIS_COLOR,
        }),
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
      buildEmptyLineSeries({ name: leftSeriesName, yAxisIndex: 0 }),
      ...(hasRightAxis
        ? [buildEmptyLineSeries({ name: rightSeriesName, yAxisIndex: 1 })]
        : []),
    ],
  };
}

function buildSingleSeriesPatch({
  displaySeriesName,
  displayChartPoints,
  markAreaData,
  yAxisIndex,
  includeMarkArea,
}) {
  const hasSinglePoint = displayChartPoints.length === 1;
  const color = getAxisColor(yAxisIndex);

  const seriesPatch = {
    name: displaySeriesName,
    yAxisIndex,
    data: displayChartPoints,
    showSymbol: hasSinglePoint,
    symbol: hasSinglePoint ? "circle" : "none",
    symbolSize: hasSinglePoint ? 7 : 0,
    lineStyle: {
      width: 2,
      color,
    },
    itemStyle: {
      color,
    },
  };

  if (includeMarkArea && markAreaData.length) {
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

/**
 * Builds the small series-only patch applied to an existing chart instance.
 * This avoids recreating the full ECharts option whenever new streamed data arrives.
 */
export function buildSeriesPatch({
  leftSeriesName,
  leftChartPoints,
  rightSeriesName = null,
  rightChartPoints = [],
  markAreaData,
}) {
  return [
    buildSingleSeriesPatch({
      displaySeriesName: leftSeriesName,
      displayChartPoints: leftChartPoints,
      markAreaData,
      yAxisIndex: 0,
      includeMarkArea: true,
    }),
    ...(rightSeriesName
      ? [
          buildSingleSeriesPatch({
            displaySeriesName: rightSeriesName,
            displayChartPoints: rightChartPoints,
            markAreaData: [],
            yAxisIndex: 1,
            includeMarkArea: false,
          }),
        ]
      : []),
  ];
}