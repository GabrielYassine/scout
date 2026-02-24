import { useState, useMemo, memo, useCallback } from "react";
import {LineChart,Line,XAxis,YAxis,CartesianGrid,Tooltip,Legend,ResponsiveContainer,} from "recharts";
import "./RunChart.css";

function RunChart({ run, runIndex, problemIndex }) {
  const iterations = run?.iterations ?? [];
  const series = run?.series ?? {};
  const keys = Object.keys(series);
  const [selectedObserver, setSelectedObserver] = useState(keys[0] || null);
  const handleObserverChange = useCallback((observerKey) => {
    setSelectedObserver(observerKey);
  }, []);

  if (!iterations.length || keys.length === 0) {
    return (
      <div className="run-chart-panel">
        <div className="run-chart-title">
            {run?.problemId}
        </div>
        <div>No data to plot.</div>
      </div>
    );
  }

  const data = useMemo(() => {
    if (!selectedObserver || !series[selectedObserver]) return [];

    const observerData = series[selectedObserver];
    const minLen = Math.min(iterations.length, observerData.length);

    return Array.from({ length: minLen }, (_, i) => ({
      iteration: iterations[i],
      [selectedObserver]: observerData[i]
    }));
  }, [selectedObserver, iterations, series]);

  return (
    <div className="chart-panel">
      <div className="chart-title">
        {run.problemId}
      </div>

      <div className="run-chart-inner">
        <ResponsiveContainer>
          <LineChart data={data}>
            <CartesianGrid stroke="#e5e5e5" strokeDasharray="3 3" />
            <XAxis dataKey="iteration" stroke="#000" tick={{ fill: "#000" }} />
            <YAxis stroke="#000" tick={{ fill: "#000" }} />

            <Tooltip/>
            <Legend />

            {selectedObserver && (
              <Line key={selectedObserver} type="monotone" dataKey={selectedObserver} dot={false} stroke="#8884d8" strokeWidth={2} />
            )}
          </LineChart>
        </ResponsiveContainer>
      </div>

      {keys.length > 1 && (
        <div className="observer-checkboxes">
          {keys.map((key) => (
            <label key={key} className="observer-checkbox-label">
              <input
                type="radio"
                name={`observer-${run.problemId}-${runIndex}`}
                checked={selectedObserver === key}
                onChange={() => handleObserverChange(key)}
              />
              <span>{key}</span>
            </label>
          ))}
        </div>
      )}
    </div>
  );
}

export default memo(RunChart);

