export function getDataXRange(points) {
  if (!points?.length) return null;

  const first = points[0]?.[0];
  const last = points[points.length - 1]?.[0];
  const min = Number(first);
  const max = Number(last);

  if (Number.isFinite(min) && Number.isFinite(max)) {
    return { min: Math.min(min, max), max: Math.max(min, max) };
  }

  const xs = points.map(([x]) => Number(x)).filter(Number.isFinite);
  if (!xs.length) return null;

  return {
    min: Math.min(...xs),
    max: Math.max(...xs),
  };
}

export function rangesEqual(a, b) {
  if (!a || !b) return false;
  return Math.abs(a.min - b.min) < 1e-9 && Math.abs(a.max - b.max) < 1e-9;
}

export function clampRange(range, fullRange) {
  if (!range || !fullRange) return fullRange ?? null;

  return {
    min: Math.max(fullRange.min, Math.min(range.min, fullRange.max)),
    max: Math.max(fullRange.min, Math.min(range.max, fullRange.max)),
  };
}

export function resolveZoomRange(zoom, fullRange) {
  if (!fullRange) return null;
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