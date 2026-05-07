/**
 * Run page controls for playback and run selection.
 * @author s235257 & s230632
 */
import "@/features/run/styles/RunControls.css";

export default function RunControls({
  currentAnimationLength,
  playbackSpeed,
  setPlaybackSpeed,
  resetPlayback,
  averageRuns,
  batches,
  effectiveSelectedRunKey,
  onSelectedRunChange,
  layoutMode,
  setLayoutMode,
}) {
  return (
    <div className="run-page-controls">
      {currentAnimationLength > 0 && (
        <div className="run-speed-control">
          <label htmlFor="playback-speed" className="field-label">
            Graph speed:
          </label>

          <div className="run-speed-slider-group">
            <input
              id="playback-speed"
              className="field-input run-speed-slider"
              type="range"
              min="1"
              max="1000"
              value={playbackSpeed}
              onChange={(e) => setPlaybackSpeed(Number(e.target.value))}
            />
            <span className="run-speed-value">{playbackSpeed}</span>
          </div>

          <button
            type="button"
            className="btn btn--red run-playback-button"
            onClick={resetPlayback}
          >
            Playback
          </button>
        </div>
      )}

      <div className="run-selection-row">
        <div className="run-selector">
          <label htmlFor="batch-select" className="field-label">
            Select Run:
          </label>

          {/* Average is available only after the backend summary has been produced. */}
          <select
            id="batch-select"
            className="field-input"
            value={effectiveSelectedRunKey ?? ""}
            onChange={(e) => onSelectedRunChange(e.target.value)}
            disabled={averageRuns.length === 0 && batches.length <= 1}
          >
            {averageRuns.length > 0 && <option value="average">Average</option>}

            {batches.map((batchItem) => (
              <option key={batchItem.runIndex} value={String(batchItem.runIndex)}>
                Run {batchItem.runIndex + 1} (Seed: {batchItem.seed})
              </option>
            ))}

            {averageRuns.length === 0 && batches.length === 0 && (
              <option value="">No runs available</option>
            )}
          </select>
        </div>

        <div className="run-layout-toggle">
          <button
            type="button"
            className="btn btn--green"
            onClick={() => setLayoutMode("stack")}
          >
            Stacked
          </button>
          <button
            type="button"
            className="btn btn--green"
            onClick={() => setLayoutMode("grid")}
          >
            Grid
          </button>
        </div>
      </div>
    </div>
  );
}