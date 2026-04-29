/**
 * Renders the hypercube observer projection for bitstring runs.
 * The observer provides x/y coordinates over time, and this component draws
 * the visited trajectory up to the current playback position.
 */
import { useMemo, memo } from "react";
import "@/features/run/styles/HypercubePlot.css";

/**
 * Builds the two boundary curves used as a visual reference for the projection.
 * The shape is not part of the optimization logic; it only gives the plot a
 * recognizable outline around the projected trajectory.
 */
function buildEyePaths(width, height, padding, gaussianScale, steps = 250) {
  const innerW = width - 2 * padding;
  const innerH = height - 2 * padding;

  const toPx = (x) => padding + ((x + 1) / 2) * innerW;
  const toPy = (y) => padding + (1 - y) * innerH;

  const left = [];
  const right = [];

  for (let i = 0; i <= steps; i++) {
    const y = i / steps;
    const u = (2 * y - 1) * gaussianScale;
    const e = Math.exp(-(u * u) / 8);

    left.push([toPx(-e), toPy(y)]);
    right.push([toPx(e), toPy(y)]);
  }

  const toPath = (pts) => pts.map(([x, y], i) => `${i === 0 ? "M" : "L"} ${x.toFixed(2)} ${y.toFixed(2)}`).join(" ");

  return { leftD: toPath(left), rightD: toPath(right) };
}

function HypercubePlot({
  run,
  width = 520,
  height = 360,
  padding = 20,
  gaussianScale = 7.0,
  eyeSteps = 250,
  visibleCount = null,
}) {
  const { pts, leftD, rightD } = useMemo(() => {
    const xs = run?.series?.hypercubeX ?? [];
    const ys = run?.series?.hypercubeY ?? [];
    const len = Math.min(xs.length, ys.length);

    if (!len) return { pts: [], leftD: "", rightD: "" };

    const innerW = width - 2 * padding;
    const innerH = height - 2 * padding;

    // Convert normalized observer coordinates into SVG pixel coordinates.
    const pts = [];

    for (let i = 0; i < len; i++) {
      const x = Number(xs[i]);
      const y = Number(ys[i]);

      if (!Number.isFinite(x) || !Number.isFinite(y)) continue;

      const px = padding + ((x + 1) / 2) * innerW;
      const py = padding + (1 - y) * innerH;

      pts.push({ i, px, py });
    }

    const { leftD, rightD } = buildEyePaths(
      width,
      height,
      padding,
      gaussianScale,
      eyeSteps
    );

    return { pts, leftD, rightD };
  }, [
    run?.series?.hypercubeX,
    run?.series?.hypercubeY,
    width,
    height,
    padding,
    gaussianScale,
    eyeSteps,
  ]);

  const visiblePts = visibleCount == null ? pts : pts.slice(0, visibleCount);

  if (!visiblePts.length) return <div>No hypercube data.</div>;

  const d = visiblePts
    .map((p, idx) => `${idx === 0 ? "M" : "L"} ${p.px.toFixed(2)} ${p.py.toFixed(2)}`)
    .join(" ");

  const last = visiblePts[visiblePts.length - 1];

  return (
    <svg
      className="hypercube-svg"
      viewBox={`0 0 ${width} ${height}`}
      preserveAspectRatio="xMidYMid meet"
    >
      <path d={leftD} fill="none" stroke="#111" strokeWidth="2" />
      <path d={rightD} fill="none" stroke="#111" strokeWidth="2" />

      <path d={d} fill="none" stroke="#999" strokeWidth="2" />

      {visiblePts.map((p) => (
        <circle key={p.i} cx={p.px} cy={p.py} r="2" fill="#3b82f6" />
      ))}

      <circle cx={last.px} cy={last.py} r="5" fill="#ef4444" />
    </svg>
  );
}

export default memo(HypercubePlot);