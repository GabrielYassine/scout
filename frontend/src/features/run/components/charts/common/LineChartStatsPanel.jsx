/**
 * Optional statistics panel for the currently visible line-chart window.
 * Falls back to local statistics if the backend stats endpoint fails.
 */
import { useCallback, useState } from "react";
import { computeLocalStats } from "./lineChartStats.js";
import "@/features/run/styles/LineCharts.css";

export default function LineChartStatsPanel({
  seriesName,
  xAxisLabel = "Evaluation",
  yAxisLabel = null,
  visiblePoints,
  windowRange,
}) {
  const [statsLoading, setStatsLoading] = useState(false);
  const [statsError, setStatsError] = useState("");
  const [windowStats, setWindowStats] = useState(null);

  const computeAndFetchStats = useCallback(async () => {
    const points = visiblePoints;

    if (!points?.length) {
      setStatsError("No points in the current window.");
      setWindowStats(null);
      return;
    }

    const payload = {
      seriesName,
      xAxisLabel,
      yAxisLabel: yAxisLabel ?? seriesName,
      xMin: windowRange?.min ?? points[0][0],
      xMax: windowRange?.max ?? points[points.length - 1][0],
      points,
    };

    setStatsLoading(true);
    setStatsError("");

    try {
      const res = await fetch("/api/stats/series-window", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        let message = `Stats request failed with status ${res.status}`;

        try {
          const data = await res.json();
          if (data?.message) message = data.message;
        } catch {
          // ignore parse failure
        }

        throw new Error(message);
      }

      const data = await res.json();
      setWindowStats(data);
    } catch (err) {
      const fallback = computeLocalStats(points);
      setWindowStats(fallback);
      setStatsError(
        err?.message ?? "Failed to fetch stats; showing local summary instead."
      );
    } finally {
      setStatsLoading(false);
    }
  }, [seriesName, xAxisLabel, yAxisLabel, visiblePoints, windowRange]);

  return (
    <div className="line-chart-stats-panel">
      <div className="line-chart-stats-actions">
        <button
          type="button"
          onClick={computeAndFetchStats}
          disabled={statsLoading || !(visiblePoints?.length > 0)}
        >
          {statsLoading ? "Computing..." : "Compute stats for zoom window"}
        </button>

        {windowRange && (
          <span className="line-chart-stats-window">
            Window: {windowRange.min} → {windowRange.max} (
            {visiblePoints?.length ?? 0} points)
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
            {windowStats.count ?? "-"}
          </div>

          <div>
            <strong>Mean</strong>
            <br />
            {Number(windowStats.mean ?? 0).toFixed(4)}
          </div>

          <div>
            <strong>Median</strong>
            <br />
            {Number(windowStats.median ?? 0).toFixed(4)}
          </div>

          <div>
            <strong>Std Dev</strong>
            <br />
            {Number(windowStats.stdDev ?? windowStats.stddev ?? 0).toFixed(4)}
          </div>

          <div>
            <strong>Min / Max</strong>
            <br />
            {Number(windowStats.min ?? 0).toFixed(4)} /{" "}
            {Number(windowStats.max ?? 0).toFixed(4)}
          </div>

          <div>
            <strong>Q1 / Q3</strong>
            <br />
            {Number(windowStats.q1 ?? 0).toFixed(4)} /{" "}
            {Number(windowStats.q3 ?? 0).toFixed(4)}
          </div>

          <div>
            <strong>IQR</strong>
            <br />
            {Number(windowStats.iqr ?? 0).toFixed(4)}
          </div>

          <div>
            <strong>Trend</strong>
            <br />
            {windowStats.trend ?? "-"} (
            {Number(windowStats.slope ?? 0).toFixed(6)})
          </div>
        </div>
      )}
    </div>
  );
}