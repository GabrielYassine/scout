import {LineChart,Line,XAxis,YAxis,CartesianGrid,Tooltip,Legend,ResponsiveContainer,} from "recharts";
import "./RunChart.css";

export default function RunChart({ run }) {
  const iterations = run?.iterations ?? [];
  const series = run?.series ?? {};
  const keys = Object.keys(series);

  if (!iterations.length || keys.length === 0) {
    return <div>No data to plot.</div>;
  }

  // If lengths mismatch, clamp to shortest length
  const minLen = Math.min(iterations.length, ...keys.map(k => (series[k]?.length ?? 0)));

  const data = Array.from({ length: minLen }, (_, i) => {
    const row = { iteration: iterations[i] };
    for (const k of keys) row[k] = series[k][i];
    return row;
  });

  return (
      <div className="run-chart-inner">
        <ResponsiveContainer>
          <LineChart data={data}>
            <CartesianGrid stroke="#e5e5e5" strokeDasharray="3 3" />
            <XAxis dataKey="iteration" stroke="#000" tick={{ fill: "#000" }} />
            <YAxis stroke="#000" tick={{ fill: "#000" }} />

            <Tooltip/>
            <Legend />

            {keys.map((k) => (
              <Line key={k} type="monotone" dataKey={k} dot={false} />
            ))}
          </LineChart>
        </ResponsiveContainer>
    </div>
  );
}
