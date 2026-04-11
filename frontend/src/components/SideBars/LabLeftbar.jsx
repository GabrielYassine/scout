import { useMemo, useState } from "react";
import "./LabLeftbar.css";
import "./FormFields.css";
import Section from "./Section.jsx";
import ParamField from "./ParamField.jsx";

const parseValue = (type, raw) => {
  if (raw == null) return raw;
  if (type === "boolean") return Boolean(raw);
  if (type === "int" || type === "long" || type === "double") {
    if (raw === "") return "";
    return Number(raw);
  }

  return raw;
};

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
  readOnly = false,
}) {
  const [open, setOpen] = useState({
    templates: true,
    global: true,
    searchSpace: true,
    problem: true,
    generator: true,
    selection: true,
    populationModel: true,
    parentSelectionRule: true,
    crossover: true,
    stopCondition: true,
    observer: true,
  });

  const disabled = readOnly || catalogLoading || !!catalogError;
  const [selectedTemplateId, setSelectedTemplateId] = useState("");
  const runMode = params.global?.experimentType ?? "run";

  const findPieceDef = (type, id) => {
    if (!catalog || !id) return null;

    const catalogMap = {
      searchSpace: catalog.searchSpaces,
      problem: catalog.problems,
      generator: catalog.generators,
      selection: catalog.selectionRules,
      populationModel: catalog.populationModels,
      parentSelectionRule: catalog.parentSelectionRules,
      crossover: catalog.crossovers,
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
      puzzleConfig.selection &&
      puzzleConfig.populationModel &&
      puzzleConfig.parentSelectionRule &&
      puzzleConfig.crossover &&
      puzzleConfig.stopCondition &&
      puzzleConfig.observer
    );
  }, [puzzleConfig]);

  const defaultSeed = 1;

  function setParam(type, def, rawValue) {
    const currentParams = params[type] ?? {};
    const parsedValue = parseValue(def.type, rawValue);

    onParamChange(type, {
      ...currentParams,
      [def.key]: parsedValue,
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

        {!readOnly && (
          <Section
            title="Templates"
            isOpen={open.templates ?? true}
            onToggle={() => setOpen((o) => ({ ...o, templates: !(o.templates ?? true) }))}
          >
            <div className="ll-subsection">
              <select
                className="field-input"
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
        )}

        {/* Global Settings Section */}
        <Section
          title="Global Settings"
          isOpen={open.global}
          onToggle={() => setOpen((o) => ({ ...o, global: !o.global }))}
        >
          <div className="ll-subsection">
            <div className="field-row">
              <label className="field-label">Mode</label>
              <select
                className="field-input"
                disabled={disabled}
                value={runMode}
                onChange={(e) =>
                  onParamChange("global", {
                    ...(params.global ?? {}),
                    experimentType: e.target.value,
                  })
                }
              >
                <option value="run">Standard Run</option>
                <option value="runtimeStudy">Runtime Study</option>
              </select>
            </div>

            <ParamField
              def={{
                key: "seed",
                label: "Seed",
                type: "long",
                min: 1,
                defaultValue: defaultSeed,
              }}
              disabled={disabled}
              value={params.global?.seed ?? defaultSeed}
              onValueChange={(v) => setParam("global", { key: "seed", type: "long" }, v)}
            />

            {runMode === "run" && (
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
                onValueChange={(v) =>
                  setParam("global", { key: "runTimes", type: "int" }, v)
                }
              />
            )}

            {runMode === "runtimeStudy" && (
              <ParamField
                def={{
                  key: "repetitionsPerSize",
                  label: "Repetitions per Size",
                  type: "int",
                  min: 1,
                  defaultValue: 1,
                }}
                disabled={disabled}
                value={params.global?.repetitionsPerSize ?? 1}
                onValueChange={(v) =>
                  setParam("global", { key: "repetitionsPerSize", type: "int" }, v)
                }
              />
            )}

            {runMode === "run" && (
              <ParamField
                def={{
                  key: "logEveryIterations",
                  label: "Backend log every X iterations",
                  type: "int",
                  min: 10,
                  defaultValue: 10,
                }}
                disabled={disabled}
                value={params.global?.logEveryIterations ?? 10}
                onValueChange={(v) =>
                  setParam("global", { key: "logEveryIterations", type: "int" }, v)
                }
              />
            )}

            {runMode === "run" && (
              <ParamField
                def={{
                  key: "wsUpdateEveryIterations",
                  label: "WebSocket update every X iterations",
                  type: "int",
                  min: 1,
                  defaultValue: 100,
                }}
                disabled={disabled}
                value={params.global?.wsUpdateEveryIterations ?? 100}
                onValueChange={(v) =>
                  setParam("global", { key: "wsUpdateEveryIterations", type: "int" }, v)
                }
              />
            )}

            {runMode === "runtimeStudy" && (
              <ParamField
                def={{
                  key: "problemSizes",
                  label: "Problem Sizes (comma separated)",
                  type: "string",
                  defaultValue: "100, 200, 400, 800",
                }}
                disabled={disabled}
                value={params.global?.problemSizes ?? "100, 200, 400, 800"}
                onValueChange={(v) =>
                  setParam("global", { key: "problemSizes", type: "string" }, v)
                }
              />
            )}
          </div>
        </Section>

        {renderPieceSection("searchSpace", "Search Space")}
        {renderPieceSection("problem", "Problem")}
        {renderPieceSection("generator", "Mutation")}
        {renderPieceSection("selection", "Selection Rule")}
        {renderPieceSection("populationModel", "Population Model")}
        {renderPieceSection("parentSelectionRule", "Parent Selection")}
        {renderPieceSection("crossover", "Crossover")}
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