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
}) {
  const [open, setOpen] = useState({
    searchSpace: true,
    problem: true,
    algorithm: true,
    mutation: true,
    acceptance: true,
    stopCondition: true,
  });

  const disabled = catalogLoading || !!catalogError;

  const findPieceDef = (type, id) => {
    if (!catalog || !id) return null;

    const catalogMap = {
      searchSpace: catalog.searchSpaces,
      problem: catalog.problems,
      algorithm: catalog.algorithms,
      mutation: catalog.mutations,
      acceptance: catalog.acceptanceRules,
      stopCondition: catalog.stopConditions,
    };

    const list = catalogMap[type] ?? [];
    return list.find((item) => item.id === id) ?? null;
  };

  // Check if all required pieces are placed
  const allPiecesPlaced = useMemo(() => {
    return (
      puzzleConfig.searchSpace &&
      puzzleConfig.problem &&
      puzzleConfig.algorithm &&
      puzzleConfig.mutation &&
      puzzleConfig.acceptance &&
      puzzleConfig.stopCondition
    );
  }, [puzzleConfig]);

  function setParam(type, def, rawValue) {
    const currentParams = params[type] ?? {};
    onParamChange(type, {
      ...currentParams,
      [def.key]: parseValue(def.type, rawValue),
    });
  }

  const renderPieceSection = (type, title) => {
    const pieces = puzzleConfig[type] || [];
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
        {renderPieceSection("searchSpace", "Search Space")}
        {renderPieceSection("problem", "Problem")}
        {renderPieceSection("algorithm", "Algorithm")}
        {renderPieceSection("mutation", "Mutation")}
        {renderPieceSection("acceptance", "Acceptance Rule")}
        {renderPieceSection("stopCondition", "Stop Condition")}
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