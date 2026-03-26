import { useState, useMemo, memo, useCallback, useEffect } from "react";
import "./RunChart.css";
import HypercubePlot from "./HypercubePlot.jsx";
import TSPVisualization from "../TSPVisualization/TSPVisualization.jsx";
import LineCharts from "./LineCharts.jsx";

const HYPERCUBE_KEY = "__hypercube__";
const TSP_TOUR_KEY = "__tsp-tour__";

function RunChart({ run, runIndex, problemIndex, playbackSpeed = 50 }) {
  const evaluations = run?.evaluations ?? [];
  const iterations = run?.iterations ?? [];
  const series = run?.series ?? {};
  const hasHypercube =(series.hypercubeX?.length ?? 0) > 0 && (series.hypercubeY?.length ?? 0) > 0;
  const hasTSPTour = (series.tspTour?.length ?? 0) > 0 && (series.tspCities?.length ?? 0) > 0;

  const keys = Object.keys(series).filter(
    (k) =>
      k !== "hypercubeX" &&
      k !== "hypercubeY" &&
      k !== "tspTour" &&
      k !== "tspCities" &&
      k !== "pheromoneHeatmap" &&
      k !== "fitnessPhaseIntervals"
  );

   const displayKeys = useMemo(() => {
    const out = [...keys];
     if (hasHypercube) out.push(HYPERCUBE_KEY);
     if (hasTSPTour) out.push(TSP_TOUR_KEY);
     return out;
   }, [keys, hasHypercube, hasTSPTour]);

  const [selectedObserver, setSelectedObserver] = useState(displayKeys[0] || (hasTSPTour ? TSP_TOUR_KEY : null) );

  const [visibleCount, setVisibleCount] = useState(1);

  const handleObserverChange = useCallback((observerKey) => {
    setSelectedObserver(observerKey);
  }, []);

  useEffect(() => {
    if (!selectedObserver || !displayKeys.includes(selectedObserver)) {
      setSelectedObserver(displayKeys[0] || (hasTSPTour ? TSP_TOUR_KEY : null));
    }
  }, [displayKeys, hasTSPTour, selectedObserver]);

  const data = useMemo(() => {
    if (
      !selectedObserver ||
      selectedObserver === HYPERCUBE_KEY ||
      selectedObserver === TSP_TOUR_KEY ||
      !series[selectedObserver]
    ) {
      return [];
    }

    const observerData = series[selectedObserver];
    const minLen = Math.min(evaluations.length, observerData.length);

    return Array.from({ length: minLen }, (_, i) => [
      Number(evaluations[i]),
      Number(observerData[i]),
    ]).filter(([x, y]) => Number.isFinite(x) && Number.isFinite(y));
  }, [selectedObserver, evaluations, series]);

  const animationLength = useMemo(() => {
    if (selectedObserver === HYPERCUBE_KEY) {
      return Math.min(
        series.hypercubeX?.length ?? 0,
        series.hypercubeY?.length ?? 0
      );
    }

    if (selectedObserver === TSP_TOUR_KEY) {
      return series.tspTour?.length ?? 0;
    }

    return data.length;
  }, [selectedObserver, series, data.length]);

  useEffect(() => {
    setVisibleCount(1);
  }, [run, selectedObserver]);

  useEffect(() => {
    if (!selectedObserver || !animationLength) return;

    const stepSize = Math.max(1, Math.floor(playbackSpeed / 15));

    const interval = setInterval(() => {
      setVisibleCount((prev) => {
        if (prev >= animationLength) return prev;
        return Math.min(prev + stepSize, animationLength);
      });
    }, 30);

    return () => clearInterval(interval);
  }, [animationLength, playbackSpeed, selectedObserver]);

  const visibleData = useMemo(() => {
    return data.slice(0, visibleCount);
  }, [data, visibleCount]);

  const phaseRanges = useMemo(() => {
    const intervals = series.fitnessPhaseIntervals ?? [];
    if (!intervals.length || !iterations.length || !evaluations.length) return [];

    const iterationToEvaluation = new Map();
    for (let i = 0; i < iterations.length; i += 1) {
      iterationToEvaluation.set(iterations[i], evaluations[i]);
    }

    const lookupEvaluation = (iteration) => {
      const direct = iterationToEvaluation.get(iteration);
      if (direct != null) return direct;
      let bestIndex = -1;
      let bestDelta = Number.POSITIVE_INFINITY;
      for (let i = 0; i < iterations.length; i += 1) {
        const delta = Math.abs(iterations[i] - iteration);
        if (delta < bestDelta) {
          bestDelta = delta;
          bestIndex = i;
        }
      }
      return bestIndex >= 0 ? evaluations[bestIndex] : null;
    };

    return intervals
      .map((interval) => {
        const startIteration = Number(interval?.startIteration);
        const endIteration = Number(interval?.endIteration);
        if (!Number.isFinite(startIteration) || !Number.isFinite(endIteration)) return null;
        const startEval = lookupEvaluation(startIteration);
        const endEval = lookupEvaluation(endIteration);
        if (!Number.isFinite(startEval) || !Number.isFinite(endEval)) return null;
        const start = Math.min(startEval, endEval);
        const end = Math.max(startEval, endEval);
        const phase = typeof interval?.phase === "string" ? interval.phase : "STAGNANT";
        return { start, end, phase };
      })
      .filter(Boolean);
  }, [series.fitnessPhaseIntervals, iterations, evaluations]);

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

  return (
    <div className="chart-panel">
      <div className="chart-title">
        {run.problemId}
      </div>

      <div className="run-chart-inner">
        {selectedObserver === HYPERCUBE_KEY ? (
          <HypercubePlot run={run} visibleCount={visibleCount} />
        ) : selectedObserver === TSP_TOUR_KEY ? (
          <TSPVisualization run={run} />
        ) :  (
          <LineCharts
            selectedObserver={selectedObserver}
            chartPoints={visibleData}
            searchSpaceId={run?.searchSpaceId}
            phaseRanges={phaseRanges}
          />
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

