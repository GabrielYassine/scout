import { useState, useMemo, memo, useCallback } from "react";
import {LineChart,Line,XAxis,YAxis,CartesianGrid,Tooltip,Legend,ResponsiveContainer,} from "recharts";
import "./RunChart.css";
import HypercubePlot from "./HypercubePlot.jsx";
import TSPVisualization from "../TSPVisualization/TSPVisualization.jsx";

const HYPERCUBE_KEY = "__hypercube__";
const TSP_TOUR_KEY = "__tsp-tour__";

function RunChart({ run, runIndex, problemIndex }) {
  const evaluations = run?.evaluations ?? [];
  const series = run?.series ?? {};
  const hasHypercube =(series.hypercubeX?.length ?? 0) > 0 && (series.hypercubeY?.length ?? 0) > 0;
  const hasTSPTour = (series.tspTour?.length ?? 0) > 0 && (series.tspCities?.length ?? 0) > 0;

  const keys = Object.keys(series).filter( (k) => k !== "hypercubeX" && k !== "hypercubeY" && k !== "tspTour" && k !== "tspCities" );

   const displayKeys = useMemo(() => {
    const out = [...keys];
     if (hasHypercube) out.push(HYPERCUBE_KEY);
     if (hasTSPTour) out.push(TSP_TOUR_KEY);
     return out;
   }, [keys, hasHypercube, hasTSPTour]);

  const [selectedObserver, setSelectedObserver] = useState(displayKeys[0] || null);
  const handleObserverChange = useCallback((observerKey) => {
    setSelectedObserver(observerKey);
  }, []);

  if (!evaluations.length || displayKeys.length === 0) {
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
    if (!selectedObserver || !series[selectedObserver]|| selectedObserver === HYPERCUBE_KEY || selectedObserver === TSP_TOUR_KEY) return [];

    const observerData = series[selectedObserver];
    const minLen = Math.min(evaluations.length, observerData.length);

    return Array.from({ length: minLen }, (_, i) => ({
      evaluation: evaluations[i],
      [selectedObserver]: observerData[i]
    }));
  }, [selectedObserver, evaluations, series]);

  return (
    <div className="chart-panel">
      <div className="chart-title">
        {run.problemId}
      </div>

      <div className="run-chart-inner">
        {selectedObserver === HYPERCUBE_KEY ? (
          <HypercubePlot run={run} />
        ) : selectedObserver === TSP_TOUR_KEY ? (
          <TSPVisualization run={run} />
        ) : (
        <ResponsiveContainer>
          <LineChart data={data}>
            <CartesianGrid stroke="#e5e5e5" strokeDasharray="3 3" />
            <XAxis dataKey="evaluation" stroke="#000" tick={{ fill: "#000" }} />
            <YAxis stroke="#000" tick={{ fill: "#000" }} />

            <Tooltip/>
            <Legend />

            {selectedObserver && (
              <Line key={selectedObserver} type="monotone" dataKey={selectedObserver} dot={false} stroke="#8884d8" strokeWidth={2} />
            )}
          </LineChart>
        </ResponsiveContainer>
        )}
      </div>

      {displayKeys.length > 1 && (
        <div className="observer-checkboxes">
          {displayKeys.map((key) => {
            // Display friendly names for special keys
            const displayName = key === HYPERCUBE_KEY ? "Hypercube" :
                               key === TSP_TOUR_KEY ? "TSP Tour" : key;

            return (
              <label key={key} className="observer-checkbox-label">
                <input
                  type="radio"
                  name={`observer-${run.problemId}-${runIndex}`}
                  checked={selectedObserver === key}
                  onChange={() => handleObserverChange(key)}
                />
                <span>{displayName}</span>
              </label>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default memo(RunChart);

