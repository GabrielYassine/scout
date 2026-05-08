/**
  * Functions for handling zoom ranges in line charts, including converting between
  * data points and zoom events, and clamping zoom ranges to the available data.
  * @author s230632
 */

// Given an array of data points, extract the x-values of the first and last points to determine the overall x-axis range of the data.
export function getDataXRange(points) {
  if (!points.length) return null;

  const firstX = Number(points[0][0]);
  const lastX = Number(points[points.length - 1][0]);

  if (!Number.isFinite(firstX) || !Number.isFinite(lastX)) {
    return null;
  }

  return {
    min: Math.min(firstX, lastX),
    max: Math.max(firstX, lastX),
  };
}

export function rangesEqual(a, b) {
  if (!a || !b) return false;

  return Math.abs(a.min - b.min) < 1e-9 && Math.abs(a.max - b.max) < 1e-9;
}

/**
 * Keeps a zoom range inside the full available data range.
 * This avoids stats calculations using x-values outside the actual chart data.
 */
export function clampRange(range, fullRange) {
  return {
    min: Math.max(fullRange.min, Math.min(range.min, fullRange.max)),
    max: Math.max(fullRange.min, Math.min(range.max, fullRange.max)),
  };
}

/**
 * ECharts dataZoom can report the selected window either as exact x-axis values
 * or as percentages. This converts both formats into numeric x-axis bounds.
 */
export function resolveZoomRange(zoom, fullRange) {
  if (!zoom) return fullRange;

  const startValue = Number(zoom.startValue);
  const endValue = Number(zoom.endValue);

  if (Number.isFinite(startValue) && Number.isFinite(endValue)) {
    return clampRange(
      {
        min: Math.min(startValue, endValue),
        max: Math.max(startValue, endValue),
      },
      fullRange
    );
  }

  const startPercent = Number(zoom.start);
  const endPercent = Number(zoom.end);

  if (Number.isFinite(startPercent) && Number.isFinite(endPercent)) {
    const startPct = Math.min(startPercent, endPercent) / 100;
    const endPct = Math.max(startPercent, endPercent) / 100;
    const span = fullRange.max - fullRange.min;

    return clampRange(
      {
        min: fullRange.min + startPct * span,
        max: fullRange.min + endPct * span,
      },
      fullRange
    );
  }

  return fullRange;
}