/**
 * Collapsible observer controls for RunChart.
 * Lets the user choose the primary left-axis line series, optional right-axis
 * line series, and special non-line visualizations.
 */
import { useState, memo } from "react";

import {
  getObserverDisplayName,
} from "../charts/run/runChartData.js";

function RunChartObserverControls({
  problemId,
  runIndex,
  lineSeriesKeys,
  specialDisplayKeys,
  activeObserver,
  rightLineSeries,
  isLineSeries,
  onLeftAxisChange,
  onRightAxisChange,
  onSpecialObserverChange,
}) {
  const [expanded, setExpanded] = useState(false);

  const hasControls =
    lineSeriesKeys.length > 0 ||
    specialDisplayKeys.length > 0;

  if (!hasControls) {
    return null;
  }

  return (
    <div className="observer-controls">
      <button
        type="button"
        className="observer-controls-toggle"
        onClick={() => setExpanded((current) => !current)}
        aria-expanded={expanded}
      >
        <span className={`observer-controls-caret ${expanded ? "expanded" : ""}`}>
          ▶
        </span>
        <span>Chart options</span>
      </button>

      {expanded && (
        <div className="observer-rows">
          {lineSeriesKeys.length > 0 && (
            <div className="observer-row">
              <div className="observer-row-title">Left y-axis</div>

              <div className="observer-checkboxes">
                {lineSeriesKeys.map((observerKey) => (
                  <label key={observerKey} className="observer-checkbox-label">
                    <input
                      type="radio"
                      name={`left-axis-${problemId}-${runIndex}`}
                      checked={isLineSeries && activeObserver === observerKey}
                      onChange={() => onLeftAxisChange(observerKey)}
                    />
                    <span>{getObserverDisplayName(observerKey)}</span>
                  </label>
                ))}
              </div>
            </div>
          )}

          {lineSeriesKeys.length > 1 && (
            <div className="observer-row">
              <div className="observer-row-title">Right y-axis</div>

              <div className="observer-checkboxes">
                <label className="observer-checkbox-label">
                  <input
                    type="radio"
                    name={`right-axis-${problemId}-${runIndex}`}
                    checked={!rightLineSeries}
                    disabled={!isLineSeries}
                    onChange={() => onRightAxisChange("")}
                  />
                  <span>None</span>
                </label>

                {lineSeriesKeys
                  .filter((observerKey) => observerKey !== activeObserver)
                  .map((observerKey) => (
                    <label key={observerKey} className="observer-checkbox-label">
                      <input
                        type="radio"
                        name={`right-axis-${problemId}-${runIndex}`}
                        checked={rightLineSeries === observerKey}
                        disabled={!isLineSeries}
                        onChange={() => onRightAxisChange(observerKey)}
                      />
                      <span>{getObserverDisplayName(observerKey)}</span>
                    </label>
                  ))}
              </div>
            </div>
          )}

          {specialDisplayKeys.length > 0 && (
            <div className="observer-row">
              <div className="observer-row-title">Other</div>

              <div className="observer-checkboxes">
                {specialDisplayKeys.map((observerKey) => (
                  <label key={observerKey} className="observer-checkbox-label">
                    <input
                      type="radio"
                      name={`special-observer-${problemId}-${runIndex}`}
                      checked={activeObserver === observerKey}
                      onChange={() => onSpecialObserverChange(observerKey)}
                    />
                    <span>{getObserverDisplayName(observerKey)}</span>
                  </label>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default memo(RunChartObserverControls);