import { useMemo, memo } from "react";
import ReactECharts from "echarts-for-react";

function LineCharts({ selectedObserver, chartPoints, searchSpaceId }) {
  const option = useMemo(() => {
    if (!selectedObserver || !chartPoints?.length) return null;

    const isTspFitness =searchSpaceId === "permutation" && (selectedObserver === "fitness" || selectedObserver === "bestFitness");

    const displayObserverName = isTspFitness ? "tourLength" : selectedObserver;

    const displayChartPoints = isTspFitness
      ? chartPoints.map(([x, y]) => [x, -y])
      : chartPoints;

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
          return `Evaluation: ${x}<br/>${displayObserverName}: ${y}`;
        },
      },
      xAxis: {
        type: "value",
        name: "Evaluation",
        nameLocation: "middle",
        nameGap: 30,
        min: "dataMin",
        max: "dataMax",
      },
      yAxis: {
        type: "value",
        name: displayObserverName,
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
          name: displayObserverName,
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
        },
      ],
    };
  }, [selectedObserver, chartPoints, searchSpaceId]);

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