import { useMemo, memo } from "react";
import ReactECharts from "echarts-for-react";

function BoxPlotChart({ problemId, seriesName, boxPlotResponse, searchSpaceId }) {
  const option = useMemo(() => {
    if (!seriesName || !boxPlotResponse) return null;

    const evaluations = Array.isArray(boxPlotResponse.evaluations)
      ? boxPlotResponse.evaluations
      : [];

    const rawBoxplots = Array.isArray(boxPlotResponse.boxplots)
      ? boxPlotResponse.boxplots
      : [];

    if (!evaluations.length || !rawBoxplots.length) return null;

    const isTspFitness =
      searchSpaceId === "permutation" &&
      (seriesName === "fitness" || seriesName === "bestFitness");

    const displaySeriesName = isTspFitness ? "tourLength" : seriesName;

    const boxplots = rawBoxplots
      .map((row) => {
        if (!Array.isArray(row) || row.length !== 5) return null;
        const values = row.map(Number);
        if (!values.every(Number.isFinite)) return null;

        if (!isTspFitness) return values;

        return [-values[4], -values[3], -values[2], -values[1], -values[0]];
      })
      .filter(Boolean);

    if (!boxplots.length) return null;

    const xLabels = evaluations.slice(0, boxplots.length).map(String);

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
          const value = params?.data ?? [];
          return [
            `${problemId}`,
            `Evaluation: ${params?.name ?? "-"}`,
            `Min: ${value[0] ?? "-"}`,
            `Q1: ${value[1] ?? "-"}`,
            `Median: ${value[2] ?? "-"}`,
            `Q3: ${value[3] ?? "-"}`,
            `Max: ${value[4] ?? "-"}`,
          ].join("<br/>");
        },
      },
      xAxis: {
        type: "category",
        data: xLabels,
        name: "Evaluation",
        nameLocation: "middle",
        nameGap: 55,
        axisLabel: {
          rotate: 45,
        },
      },
      yAxis: {
        type: "value",
        name: displaySeriesName,
        nameLocation: "middle",
        nameGap: 45,
        scale: true,
      },
      series: [
        {
          name: `${displaySeriesName} boxplot`,
          type: "boxplot",
          data: boxplots,
        },
      ],
    };
  }, [problemId, seriesName, boxPlotResponse, searchSpaceId]);

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