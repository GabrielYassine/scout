import { useMemo, useState } from "react";
import "./LabLeftbar.css";
import Section from "./Section.jsx";
import ParamField, { parseValue } from "./ParamField.jsx";

// Left sidebar for the lab page, containing problem and algorithm selection and configuration

export default function LabLeftbar({
  form,
  onChange,
  onReset,
  catalog,
  catalogLoading,
  catalogError,
}) {
  const [open, setOpen] = useState({
    problem: true,
    algorithm: true,
  });

  const problems = catalog?.problems ?? [];
  const algorithms = catalog?.algorithms ?? [];
  const disabled = catalogLoading || !!catalogError;

  const selectedProblem = useMemo(
    () => problems.find((p) => p.id === form.problem) ?? null,
    [problems, form.problem]
  );

  const selectedAlgorithm = useMemo(
    () => algorithms.find((a) => a.id === form.algorithm) ?? null,
    [algorithms, form.algorithm]
  );

  const hasProblem = !!selectedProblem;
  const hasAlgo = !!selectedAlgorithm;

  const problemParams = form.problemParams ?? {};
  const algorithmParams = form.algorithmParams ?? {};


  // Setter for problem parameter values
  // @author s235257
  function setProblemParam(def, rawValue) {
    onChange({
      ...form,
      problemParams: {
        ...problemParams,
        [def.key]: parseValue(def.type, rawValue),
      },
    });
  }

  // Setter for algorithm parameter values
  // @author s235257
  function setAlgorithmParam(def, rawValue) {
    onChange({
      ...form,
      algorithmParams: {
        ...algorithmParams,
        [def.key]: parseValue(def.type, rawValue),
      },
    });
  }

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
            disabled={disabled}
            onChange={(e) => onChange({ ...form, problem: e.target.value })}
          >
            {catalogLoading && <option value={form.problem}>Loading…</option>}

            {!catalogLoading && !hasProblem && (
              <option value={form.problem}>Unknown ({form.problem})</option>
            )}

            {!catalogLoading &&
              problems.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
          </select>
        </label>

        {!catalogLoading && selectedProblem?.params?.length > 0 && (
          <div className="ll-subsection">
            {selectedProblem.params.map((def) => (
              <ParamField
                key={def.key}
                def={def}
                disabled={disabled}
                value={
                  problemParams[def.key] !== undefined
                    ? problemParams[def.key]
                    : def.defaultValue
                }
                onValueChange={(v) => setProblemParam(def, v)}
              />
            ))}
          </div>
        )}
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
            disabled={disabled}
            onChange={(e) => onChange({ ...form, algorithm: e.target.value })}
          >
            {catalogLoading && <option value={form.algorithm}>Loading…</option>}

            {!catalogLoading && !hasAlgo && (
              <option value={form.algorithm}>Unknown ({form.algorithm})</option>
            )}

            {!catalogLoading &&
              algorithms.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.name}
                </option>
              ))}
          </select>
        </label>

        {!catalogLoading && selectedAlgorithm?.params?.length > 0 && (
          <div className="ll-subsection">
            {selectedAlgorithm.params.map((def) => (
              <ParamField
                key={def.key}
                def={def}
                disabled={disabled}
                value={
                  algorithmParams[def.key] !== undefined
                    ? algorithmParams[def.key]
                    : def.defaultValue
                }
                onValueChange={(v) => setAlgorithmParam(def, v)}
              />
            ))}
          </div>
        )}
      </Section>

      <div className="ll-actions">
        <button className="btn btn--green" type="button">
          Run
        </button>

        <button className="btn btn--red" type="button" onClick={onReset}>
          Reset
        </button>
      </div>
    </section>
  );
}