import { useState } from "react";
import "./LabLeftbar.css";

function Section({ title, isOpen, onToggle, children }) {
  return (
    <div className="ll-section">
      <button
        type="button"
        className="ll-section-header"
        onClick={onToggle}
        aria-expanded={isOpen}
      >
        <span className="ll-section-title">{title}</span>
        <span className={isOpen ? "ll-triangle open" : "ll-triangle"}>▸</span>
      </button>

      {isOpen && <div className="ll-section-body">{children}</div>}
    </div>
  );
}

export default function LabLeftbar({ form, onChange, onReset }) {
  const [open, setOpen] = useState({
    problem: true,
    algorithm: true,
    budget: true,
  });

  return (
    <section className="lab-leftbar">
      <div className="ll-title">Configuration</div>

      <Section
        title="Problem"
        isOpen={open.problem}
        onToggle={() => setOpen((o) => ({ ...o, problem: !o.problem }))}
      >
        <label className="ll-field">
          <span className="ll-label">Problem</span>
          <select
            value={form.problem}
            onChange={(e) => onChange({ ...form, problem: e.target.value })}
          >
            <option value="onemax">OneMax</option>
            <option value="leading-ones">Leading Ones</option>
          </select>
        </label>
      </Section>

      <Section
        title="Algorithm"
        isOpen={open.algorithm}
        onToggle={() => setOpen((o) => ({ ...o, algorithm: !o.algorithm }))}
      >
        <label className="ll-field">
          <span className="ll-label">Algorithm</span>
          <select
            value={form.algorithm}
            onChange={(e) => onChange({ ...form, algorithm: e.target.value })}
          >
            <option value="1p1-ea">(1+1) EA</option>
            <option value="sa">Simulated Annealing</option>
          </select>
        </label>
      </Section>

      <Section
        title="Budget"
        isOpen={open.budget}
        onToggle={() => setOpen((o) => ({ ...o, budget: !o.budget }))}
      >
        <label className="ll-field">
          <span className="ll-label">Budget</span>
          <input
            type="number"
            value={form.budget}
            onChange={(e) => onChange({ ...form, budget: Number(e.target.value) })}
          />
        </label>
      </Section>

      <div className="ll-actions">
        <button className="btn btn--green" type="button">Run</button>

        <button className="btn btn--red" type="button" onClick={onReset}>Reset</button>
      </div>
    </section>
  );
}
