export function computePercentile(sorted, p) {
  if (!sorted.length) return 0;
  if (sorted.length === 1) return sorted[0];

  const index = (p / 100) * (sorted.length - 1);
  const lower = Math.floor(index);
  const upper = Math.ceil(index);

  if (lower === upper) return sorted[lower];

  const fraction = index - lower;
  return sorted[lower] + fraction * (sorted[upper] - sorted[lower]);
}

export function computeLocalStats(points) {
  if (!points?.length) return null;

  const ys = points.map(([, y]) => Number(y)).filter(Number.isFinite);
  const xs = points.map(([x]) => Number(x)).filter(Number.isFinite);

  if (!ys.length || !xs.length) return null;

  const sorted = [...ys].sort((a, b) => a - b);
  const count = ys.length;
  const mean = ys.reduce((sum, v) => sum + v, 0) / count;
  const variance = ys.reduce((sum, v) => sum + (v - mean) ** 2, 0) / count;
  const stdDev = Math.sqrt(variance);
  const min = sorted[0];
  const max = sorted[sorted.length - 1];
  const median = computePercentile(sorted, 50);
  const q1 = computePercentile(sorted, 25);
  const q3 = computePercentile(sorted, 75);
  const iqr = q3 - q1;

  const xMean = xs.reduce((sum, v) => sum + v, 0) / count;
  const covariance =
    points.reduce((sum, [x, y]) => {
      const nx = Number(x);
      const ny = Number(y);
      return sum + (nx - xMean) * (ny - mean);
    }, 0) / count;

  const xVariance =
    xs.reduce((sum, v) => sum + (v - xMean) ** 2, 0) / count;

  const slope = xVariance === 0 ? 0 : covariance / xVariance;
  const trend = slope > 0.0001 ? "up" : slope < -0.0001 ? "down" : "flat";

  return { count, min, max, mean, stdDev, median, q1, q3, iqr, slope, trend };
}