/**
 * Displays runtime-study progress as either mean evaluations per problem size
 * or boxplots over repeated runs.
 */
import { useMemo, useState, memo } from "react";
import "@/features/run/styles/ChartPanel.css";
import "@/features/run/styles/RunChart.css";

import LineCharts from "../common/LineCharts.jsx";
import BoxPlotChart from "../common/BoxPlotChart.jsx";
import RunChartHeader from "../run/RunChartHeader.jsx";

const VIEW_LINE = "line";
const VIEW_BOXPLOT = "boxplot";

function getStudyStatusMeta(studyStatus) {
  const rawStatus = String(studyStatus ?? "").toUpperCase();

  if (rawStatus === "FINISHED") {
    return { label: "Finished", className: "finished" };
  }

  if (rawStatus === "FAILED") {
    return { label: "Failed", className: "failed" };
  }

  return { label: "Running", className: "ongoing" };
}

function RuntimeStudyChart({
  studyTitle = "Runtime Study",
  problemId = null,
  points = [],
  studyStatus = "ONGOING",
  visibleCount = null,
}) {
  const [viewMode, setViewMode] = useState(VIEW_LINE);

  const sortedPoints = useMemo(
    () =>
      [...points].sort(
        (a, b) => Number(a.problemSize) - Number(b.problemSize)
      ),
    [points]
  );

  const visiblePoints = useMemo(() => {
    if (!Number.isFinite(visibleCount) || visibleCount == null) {
      return sortedPoints;
    }
    return sortedPoints.slice(0, visibleCount);
  }, [sortedPoints, visibleCount]);

  const linePoints = useMemo(
    () =>
      visiblePoints
        .map((p) => [
          Number(p.problemSize),
          Number(p.meanEvaluationsToOptimum),
        ])
        .filter(([x, y]) => Number.isFinite(x) && Number.isFinite(y)),
    [visiblePoints]
  );

  const boxPlotResponse = useMemo(() => {
    const valid = visiblePoints.filter(
      (p) =>
        Number.isFinite(Number(p.problemSize)) &&
        Array.isArray(p.boxPlot) &&
        p.boxPlot.length === 5 &&
        p.boxPlot.every((v) => Number.isFinite(Number(v)))
    );

    return {
      xValues: valid.map((p) => p.problemSize),
      boxplots: valid.map((p) => p.boxPlot.map(Number)),
    };
  }, [visiblePoints]);

  const hasLineData = linePoints.length > 0;
  const hasBoxPlotData =
    Array.isArray(boxPlotResponse.xValues) &&
    Array.isArray(boxPlotResponse.boxplots) &&
    boxPlotResponse.xValues.length > 0 &&
    boxPlotResponse.boxplots.length > 0;

  const title = problemId ? `${studyTitle} - ${problemId}` : studyTitle;
  const statusMeta = getStudyStatusMeta(studyStatus);

  if (!hasLineData && !hasBoxPlotData) {
    return (
      <div className="run-chart-panel">
        <RunChartHeader
          problemId={title}
          statusMeta={statusMeta}
          showRuntime={false}
        />

        <div>
          {statusMeta.className === "ongoing"
            ? "Waiting for first study point..."
            : "No study data to plot."}
        </div>
      </div>
    );
  }

  return (
    <div className="chart-panel">
      <RunChartHeader
        problemId={title}
        statusMeta={statusMeta}
        showRuntime={false}
      />

      <div className="run-chart-inner">
        {viewMode === VIEW_BOXPLOT ? (
          <BoxPlotChart
            seriesName="evaluationsToOptimum"
            boxPlotResponse={boxPlotResponse}
            xAxisLabel="Problem Size"
            yAxisLabel="Evaluations to Optimum"
            invertPermutationFitness={false}
          />
        ) : (
          <LineCharts
            seriesName="avgEvaluationsToOptimum"
            chartPoints={linePoints}
            xAxisLabel="Problem Size"
            yAxisLabel="Average Evaluations to Optimum"
            invertPermutationFitness={false}
          />
        )}
      </div>

      {hasLineData && hasBoxPlotData && (
        <div className="observer-checkboxes">
          <label className="observer-checkbox-label">
            <input
              type="radio"
              name="runtime-study-view"
              checked={viewMode === VIEW_LINE}
              onChange={() => setViewMode(VIEW_LINE)}
            />
            <span>Average evaluations per problem size</span>
          </label>

          <label className="observer-checkbox-label">
            <input
              type="radio"
              name="runtime-study-view"
              checked={viewMode === VIEW_BOXPLOT}
              onChange={() => setViewMode(VIEW_BOXPLOT)}
            />
            <span>Boxplot</span>
          </label>
        </div>
      )}
    </div>
  );
}

export default memo(RuntimeStudyChart);