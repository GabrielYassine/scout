import "./RunChartHeader.css";

export default function RunChartHeader({
  problemId,
  instanceName = null,
  statusMeta,
  runtimeMs = null,
  showRuntime = true,
  dotFirst = false,
}) {
  return (
    <div className="run-chart-header">
      <div className="chart-title-row">
        <div className="chart-title">
          {problemId}
          {instanceName && (
            <span className="chart-instance-name"> — {instanceName}</span>
          )}
        </div>

        <div className={`run-status-indicator ${statusMeta.className}`}>
          {dotFirst && <span className="run-status-dot" />}
          <span className="run-status-text">{statusMeta.label}</span>
          {!dotFirst && <span className="run-status-dot" />}
        </div>
      </div>

      {showRuntime && (
        <div className="run-chart-subtitle">
          Runtime: {runtimeMs != null ? `${runtimeMs.toFixed(2)} ms` : "Running..."}
        </div>
      )}
    </div>
  );
}