/**
 * ECharts boxplot wrapper for aggregated runtime/fitness distributions.
 */
import { useMemo, memo } from "react";
import ReactECharts from "echarts-for-react";

function getBoxplotXAxisValues(boxPlotResponse) {
  if (Array.isArray(boxPlotResponse.xValues)) {
    return boxPlotResponse.xValues;
  }

  if (Array.isArray(boxPlotResponse.evaluations)) {
    return boxPlotResponse.evaluations;
  }

  return [];
}

function normalizeBoxplotRow(row, invertValues) {
  if (!Array.isArray(row) || row.length !== 5) return null;

  const values = row.map(Number);
  if (!values.every(Number.isFinite)) return null;

  return invertValues ? [-values[4], -values[3], -values[2], -values[1], -values[0]] : values;
}

function buildBoxplotEntries(xValues, rawBoxplots, invertValues) {
  return xValues
    .map((x, index) => {
      const normalized = normalizeBoxplotRow(rawBoxplots[index], invertValues);
      if (!normalized) return null;

      const [min, q1, median, q3, max] = normalized;

      return {
        label: String(x),
        item: {
          value: normalized,
          rawStats: { min, q1, median, q3, max },
        },
      };
    })
    .filter(Boolean);
}

function BoxPlotChart({
  seriesName,
  boxPlotResponse,
  searchSpaceId = null,
  xAxisLabel = "Evaluation",
  yAxisLabel = null,
  invertPermutationFitness = true,
}) {
  const option = useMemo(() => {
    if (!seriesName || !boxPlotResponse) return null;

    const xValues = getBoxplotXAxisValues(boxPlotResponse);
    const rawBoxplots = Array.isArray(boxPlotResponse.boxplots) ? boxPlotResponse.boxplots : [];

    if (!xValues.length || !rawBoxplots.length) return null;

    const isPermutationFitness =
      invertPermutationFitness &&
      (searchSpaceId === "permutation" || searchSpaceId === "route-list") &&
      (seriesName === "fitness" || seriesName === "bestFitness");

    const displaySeriesName = isPermutationFitness ? "tourLength" : seriesName;
    const resolvedYAxisLabel = yAxisLabel ?? displaySeriesName;

    const entries = buildBoxplotEntries(
      xValues,
      rawBoxplots,
      isPermutationFitness
    );

    if (!entries.length) return null;

    const labels = entries.map((entry) => entry.label);
    const seriesData = entries.map((entry) => entry.item);

    return {
      animation: false,
      grid: {
        top: 24,
        right: 24,
        bottom: 80,
        left: 64,
        containLabel: true,
      },
      tooltip: {
        trigger: "item",
        formatter: (params) => {
          const { min, q1, median, q3, max } = params?.data?.rawStats ?? {};

          return [
            displaySeriesName,
            `${xAxisLabel}: ${params?.name ?? "-"}`,
            `Min: ${Number.isFinite(min) ? min : "-"}`,
            `Q1: ${Number.isFinite(q1) ? q1 : "-"}`,
            `Median: ${Number.isFinite(median) ? median : "-"}`,
            `Q3: ${Number.isFinite(q3) ? q3 : "-"}`,
            `Max: ${Number.isFinite(max) ? max : "-"}`,
          ].join("<br/>");
        },
      },
      xAxis: {
        type: "category",
        data: labels,
        name: xAxisLabel,
        nameLocation: "middle",
        nameGap: 55,
        axisLabel: {
          rotate: 45,
        },
      },
      yAxis: {
        type: "value",
        name: resolvedYAxisLabel,
        nameLocation: "middle",
        nameGap: 45,
        scale: true,
      },
      series: [
        {
          name: `${displaySeriesName} boxplot`,
          type: "boxplot",
          data: seriesData,
        },
      ],
    };
  }, [
    seriesName,
    boxPlotResponse,
    searchSpaceId,
    xAxisLabel,
    yAxisLabel,
    invertPermutationFitness,
  ]);

  if (!option) {
    return <div>No boxplot data.</div>;
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

export default memo(BoxPlotChart);