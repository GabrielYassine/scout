/**
 * Stats panel for line chart zoom windows.
 * @author s235257
 */

import { useCallback, useState } from "react";
import "@/features/run/styles/LineCharts.css";

export default function LineChartStatsPanel({
  seriesName,
  xAxisLabel = "Evaluation",
  yAxisLabel = null,
  visiblePoints = [],
  windowRange,
}) {
  const [statsLoading, setStatsLoading] = useState(false);
  const [statsError, setStatsError] = useState("");
  const [windowStats, setWindowStats] = useState(null);

  // Fetch stats for the current zoom window from the backend API.
  const computeStats = useCallback(async () => {
    if (visiblePoints.length === 0) {
      setStatsError("No points in the current window.");
      setWindowStats(null);
      return;
    }

    const payload = {
      seriesName,
      xAxisLabel,
      yAxisLabel: yAxisLabel ?? seriesName,
      xMin: windowRange?.min ?? visiblePoints[0][0],
      xMax: windowRange?.max ?? visiblePoints[visiblePoints.length - 1][0],
      points: visiblePoints,
    };

    setStatsLoading(true);
    setStatsError("");

    try {
      const response = await fetch("/api/stats/series-window", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        throw new Error(`Stats request failed with status ${response.status}`);
      }

      const data = await response.json();
      setWindowStats(data);
    } catch (error) {
      setWindowStats(null);
      setStatsError(error.message);
    } finally {
      setStatsLoading(false);
    }
  }, [seriesName, xAxisLabel, yAxisLabel, visiblePoints, windowRange]);

  return (
    <div className="line-chart-stats-panel">
      <div className="line-chart-stats-actions">
        <div className="line-chart-stats-button-wrap">
          <button
            type="button"
            className="btn btn--green"
            onClick={computeStats}
            disabled={statsLoading || visiblePoints.length === 0}
          >
            {statsLoading ? "Computing..." : "Compute stats for zoom window"}
          </button>
        </div>

        {windowRange && (
          <span className="line-chart-stats-window">
            Window: {windowRange.min} → {windowRange.max} (
            {visiblePoints.length} points)
          </span>
        )}
      </div>

      {statsError && (
        <div className="line-chart-stats-error">{statsError}</div>
      )}

      {windowStats && (
        <div className="line-chart-stats-grid">
          <div>
            <strong>Count</strong>
            <br />
            {windowStats.count}
          </div>

          <div>
            <strong>Mean</strong>
            <br />
            {Number(windowStats.mean).toFixed(4)}
          </div>

          <div>
            <strong>Median</strong>
            <br />
            {Number(windowStats.median).toFixed(4)}
          </div>

          <div>
            <strong>Std Dev</strong>
            <br />
            {Number(windowStats.stdDev).toFixed(4)}
          </div>

          <div>
            <strong>Min / Max</strong>
            <br />
            {Number(windowStats.min).toFixed(4)} /{" "}
            {Number(windowStats.max).toFixed(4)}
          </div>

          <div>
            <strong>Q1 / Q3</strong>
            <br />
            {Number(windowStats.q1).toFixed(4)} /{" "}
            {Number(windowStats.q3).toFixed(4)}
          </div>

          <div>
            <strong>IQR</strong>
            <br />
            {Number(windowStats.iqr).toFixed(4)}
          </div>

          <div>
            <strong>Trend</strong>
            <br />
            {windowStats.trend} ({Number(windowStats.slope).toFixed(6)})
          </div>
        </div>
      )}
    </div>
  );
}