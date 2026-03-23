import { useMemo, useState } from "react";
import "./LabLeftbar.css";
import Section from "./Section.jsx";
import ParamField, { parseValue } from "./ParamField.jsx";

// Left sidebar for the lab page, containing configuration for each puzzle piece

export default function LabLeftbar({
  puzzleConfig,
  params,
  onParamChange,
  onReset,
  onRun,
  catalog,
  catalogLoading,
  catalogError,
  templates = [],
  templatesLoading = false,
  templatesError = null,
  onApplyTemplate,
}) {
  const [open, setOpen] = useState({
    templates: true,
    global: true,
    searchSpace: true,
    problem: true,
    generator: true,
    acceptance: true,
    populationModel: true,
    stopCondition: true,
    observer: true,
  });

  const disabled = catalogLoading || !!catalogError;
  const [selectedTemplateId, setSelectedTemplateId] = useState("");
  const findPieceDef = (type, id) => {
    if (!catalog || !id) return null;

    const catalogMap = {
      searchSpace: catalog.searchSpaces,
      problem: catalog.problems,
      generator: catalog.generators,
      acceptance: catalog.acceptanceRules,
      populationModel: catalog.populationModels,
      stopCondition: catalog.stopConditions,
      observer: catalog.observers,
    };

    const list = catalogMap[type] ?? [];
    return list.find((item) => item.id === id) ?? null;
  };

  // Check if all required pieces are placed
  const allPiecesPlaced = useMemo(() => {
    return (
      puzzleConfig.searchSpace &&
      puzzleConfig.problem &&
      puzzleConfig.generator &&
      puzzleConfig.populationModel &&
      puzzleConfig.acceptance &&
      puzzleConfig.stopCondition &&
      puzzleConfig.observer
    );
  }, [puzzleConfig]);

  function setParam(type, def, rawValue) {
    const currentParams = params[type] ?? {};
    const parsedValue = parseValue(def.type, rawValue);
    let valueToStore = parsedValue;

    // clamp numeric values to respect declared min/max bounds
    if (def.type === "int" || def.type === "long" || def.type === "double") {
      if (parsedValue !== "" && parsedValue != null) {
        if (def.min !== undefined) valueToStore = Math.max(parsedValue, def.min);
        if (def.max !== undefined) valueToStore = Math.min(valueToStore, def.max);
      }
    }

    onParamChange(type, {
      ...currentParams,
      [def.key]: valueToStore,
    });
  }

  const renderPieceSection = (type, title) => {
    const pieces = Array.isArray(puzzleConfig[type]) ? puzzleConfig[type] : [];
    if (pieces.length === 0) return null;

    return (
      <Section
        key={type}
        title={title}
        isOpen={open[type]}
        onToggle={() => setOpen((o) => ({ ...o, [type]: !o[type] }))}
      >
        {pieces.map((piece, index) => {
          const pieceDef = findPieceDef(type, piece.id);
          const pieceParams = params[type] ?? {};

          return (
            <div key={`${piece.id}-${index}`} className="ll-piece-container">
              <div className="ll-selected-piece">
                {pieces.length > 1 && <span className="ll-piece-number">{index + 1}.</span>}
                {piece.label}
              </div>

              {!catalogLoading && pieceDef?.params?.length > 0 && (
                <div className="ll-subsection">
                  {pieceDef.params.map((def) => (
                    <ParamField
                      key={def.key}
                      def={def}
                      disabled={disabled}
                      value={
                        pieceParams[def.key] !== undefined
                          ? pieceParams[def.key]
                          : def.defaultValue
                      }
                      onValueChange={(v) => setParam(type, def, v)}
                    />
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </Section>
    );
  };

  return (
    <section className="lab-leftbar">
      <div className="ll-content">
        <div className="ll-title">Configuration</div>

        {/* Templates Section */}
        <Section
          title="Templates"
          isOpen={open.templates ?? true}
          onToggle={() => setOpen((o) => ({ ...o, templates: !(o.templates ?? true) }))}
        >
          <div className="ll-subsection">
            <select
              className="form-select"
              value={selectedTemplateId}
              onChange={(e) => setSelectedTemplateId(e.target.value)}
              disabled={disabled || templatesLoading || templates.length === 0}
            >
              <option value="">Select template</option>
              {templates.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.displayName}
                </option>
              ))}
            </select>

            <button
              className="btn btn--green .ll-subsection"
              type="button"
              disabled={disabled || !selectedTemplateId}
              onClick={() => {
                onApplyTemplate?.(selectedTemplateId);
                setSelectedTemplateId("");
              }}
            >
              Apply Template
            </button>

            {templatesError && (
              <div className="ll-subsection">
                Failed to load templates: {templatesError}
              </div>
            )}
          </div>
        </Section>

        {/* Global Settings Section */}
        <Section
          title="Global Settings"
          isOpen={open.global}
          onToggle={() => setOpen((o) => ({ ...o, global: !o.global }))}
        >
          <div className="ll-subsection">
            <ParamField
              def={{
                key: "seed",
                label: "Seed",
                type: "long",
                min: 1,
                defaultValue: Date.now(),
              }}
              disabled={disabled}
              value={params.global?.seed ?? Date.now()}
              onValueChange={(v) => setParam("global", { key: "seed", type: "long" }, v)}
            />
            <ParamField
              def={{
                key: "runTimes",
                label: "Run Times",
                type: "int",
                min: 1,
                defaultValue: 1,
              }}
              disabled={disabled}
              value={params.global?.runTimes ?? 1}
              onValueChange={(v) => setParam("global", { key: "runTimes", type: "int" }, v)}
            />
            <ParamField
              def={{
                key: "logEveryIterations",
                label: "Log every X iterations",
                type: "int",
                defaultValue: 100,
              }}
              disabled={disabled}
              value={params.global?.logEveryIterations ?? 100}
              onValueChange={(v) =>
                setParam("global", { key: "logEveryIterations", type: "int" }, v)
              }
            />
          </div>
        </Section>

        {renderPieceSection("searchSpace", "Search Space")}
        {renderPieceSection("problem", "Problem")}
        {renderPieceSection("generator", "Mutation")}
        {renderPieceSection("acceptance", "Acceptance Rule")}
        {renderPieceSection("populationModel", "Population Model")}
        {renderPieceSection("stopCondition", "Stop Condition")}
        {renderPieceSection("observer", "Observer")}
      </div>

      <div className="ll-actions">
        <button
          className="btn btn--green"
          type="button"
          onClick={onRun}
          disabled={!allPiecesPlaced || disabled}
        >
          Run
        </button>

        <button className="btn btn--red" type="button" onClick={onReset}>
          Reset
        </button>
      </div>
    </section>
  );
}