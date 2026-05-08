/**
  *  A React component that renders a boxplot chart using ECharts, based on backend response data.
  * @author s230632s
 */

import { useMemo, memo } from "react";
import ReactECharts from "echarts-for-react";

/**
 * Supports both naming conventions used by backend summaries:
 * - xValues for generic boxplots
 * - evaluations for fitness-over-evaluation boxplots
 */
function getBoxplotXAxisValues(boxPlotResponse) {
  if (Array.isArray(boxPlotResponse.xValues)) {
    return boxPlotResponse.xValues;
  }

  if (Array.isArray(boxPlotResponse.evaluations)) {
    return boxPlotResponse.evaluations;
  }

  return [];
}

function isMinimizationFitnessBoxplot(seriesName, searchSpaceId, invertPermutationFitness) {
  return (
    invertPermutationFitness &&
    (seriesName === "fitness" || seriesName === "bestFitness") &&
    (searchSpaceId === "permutation" || searchSpaceId === "route-list")
  );
}

/**
 * Normalizes one backend boxplot row.
 * Expected row format is [min, q1, median, q3, max].
 * For TSP/VRP fitness, values may be stored as negative distances because the
 * framework maximizes fitness. In that case, the row is inverted and reordered
 * so the chart displays positive tour lengths correctly.
 */
function normalizeBoxplotRow(row, invertValues) {
  if (!Array.isArray(row) || row.length !== 5) return null;

  const values = row.map(Number);
  if (!values.every(Number.isFinite)) return null;

  return invertValues ? [-values[4], -values[3], -values[2], -values[1], -values[0]] : values;
}

/**
 * Converts backend x-values and boxplot rows into the object format expected by ECharts.
 * rawStats is kept separately so the tooltip can display named statistics.
 */
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

    // TSP and VRP are represented as minimization problems internally by using
    // negative fitness values. The chart should show positive distances instead.
    const shouldInvertFitness = isMinimizationFitnessBoxplot(
      seriesName,
      searchSpaceId,
      invertPermutationFitness
    );

    const displaySeriesName = shouldInvertFitness ? "tourLength" : seriesName;
    const resolvedYAxisLabel = yAxisLabel ?? displaySeriesName;

    const entries = buildBoxplotEntries(xValues, rawBoxplots, shouldInvertFitness);

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
          const { min, q1, median, q3, max } = params.data.rawStats;

          return [
            displaySeriesName,
            `${xAxisLabel}: ${params.name}`,
            `Min: ${min}`,
            `Q1: ${q1}`,
            `Median: ${median}`,
            `Q3: ${q3}`,
            `Max: ${max}`,
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