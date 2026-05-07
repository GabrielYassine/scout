/**
  * Component for visualizing runtime study data, including average evaluations to optimum and boxplot distributions.
  * @author s230632
 */
import { useMemo, useState, memo } from "react";
import "@/features/run/styles/ChartPanel.css";
import "@/features/run/styles/RunChart.css";

import LineCharts from "../common/LineCharts.jsx";
import BoxPlotChart from "../common/BoxPlotChart.jsx";
import RunChartHeader from "../run/RunChartHeader.jsx";

const VIEW_LINE = "line";
const VIEW_BOXPLOT = "boxplot";
// Maps study status to display label and CSS class for styling
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
// Main component for rendering the runtime study chart with options for line and boxplot views
function RuntimeStudyChart({
  studyTitle = "Runtime Study",
  problemId = null,
  points = [],
  studyStatus = "ONGOING",
  visibleCount = null,
}) {
  const [viewMode, setViewMode] = useState(VIEW_LINE);
// Sorts the study points by problem size to ensure consistent chart rendering even if data arrives out of order
  const sortedPoints = useMemo(
    () =>
      [...points].sort(
        (a, b) => Number(a.problemSize) - Number(b.problemSize)
      ),
    [points]
  );
// Determines which points to show based on visibleCount, defaulting to all if not a finite number
  const visiblePoints = useMemo(() => {
    if (!Number.isFinite(visibleCount) || visibleCount == null) {
      return sortedPoints;
    }

    return sortedPoints.slice(0, visibleCount);
  }, [sortedPoints, visibleCount]);
// Prepares data for the line chart, ensuring only valid numeric points are included
  const linePoints = useMemo(
    () =>
      visiblePoints
        .map((point) => [
          Number(point.problemSize),
          Number(point.meanEvaluationsToOptimum),
        ])
        .filter(([x, y]) => Number.isFinite(x) && Number.isFinite(y)),
    [visiblePoints]
  );
// Prepares data for the boxplot chart, ensuring only valid points with proper boxplot arrays are included
  const boxPlotResponse = useMemo(() => {
    const validPoints = visiblePoints.filter(
      (point) =>
        Number.isFinite(Number(point.problemSize)) &&
        Array.isArray(point.boxPlot) &&
        point.boxPlot.length === 5 &&
        point.boxPlot.every((value) => Number.isFinite(Number(value)))
    );

    return {
      xValues: validPoints.map((point) => point.problemSize),
      boxplots: validPoints.map((point) => point.boxPlot.map(Number)),
    };
  }, [visiblePoints]);

  const hasLineData = linePoints.length > 0;
  const hasBoxPlotData =
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