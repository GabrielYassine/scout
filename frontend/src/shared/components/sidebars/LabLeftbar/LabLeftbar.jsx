/**
  *
  * @author s235257 & s230632
 */

import { useMemo, useState } from "react";

import "@/shared/components/styles/LabLeftbar.css";
import "@/shared/components/styles/FormFields.css";

import GlobalSettingsSection from "./GlobalSettingsSection.jsx";
import SelectedPieceSection from "./SelectedPieceSection.jsx";
import TemplateSection from "./TemplateSection.jsx";

import { countPlacedPieces, findPieceDef, parseValue, } from "./labLeftbarHelpers.js";

const PIECE_SECTIONS = [
  ["searchSpace", "Search Space"],
  ["problem", "Problem"],
  ["generator", "Generator"],
  ["selection", "Selection Rule"],
  ["populationModel", "Population Model"],
  ["parentSelectionRule", "Parent Selection"],
  ["crossover", "Crossover"],
  ["stopCondition", "Stop Condition"],
  ["observer", "Observer"],
];

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

  const [showModeConfirm, setShowModeConfirm] = useState(false);
  const [pendingMode, setPendingMode] = useState(null);
  const [selectedTemplateId, setSelectedTemplateId] = useState("");

  const disabled = readOnly || catalogLoading || !!catalogError;
  const runMode = params.global?.experimentType ?? "run";

 const placedPieceCount = countPlacedPieces(puzzleConfig);

  function setParam(type, def, rawValue) {
    const currentParams = params[type] ?? {};
    const parsedValue = parseValue(def.type, rawValue);

    onParamChange(type, {...currentParams, [def.key]: parsedValue,});
  }

  function handleModeChange(nextMode) {
    const currentMode = params.global?.experimentType ?? "run";
    if (nextMode === currentMode) return;

    const switchingToRuntimeStudy = nextMode === "runtimeStudy";

    if (switchingToRuntimeStudy && placedPieceCount > 0) {
      setPendingMode(nextMode);
      setShowModeConfirm(true);
      return;
    }

    onParamChange("global", { ...(params.global ?? {}), experimentType: nextMode,  });
  }

  function confirmModeChange() {
    if (!pendingMode) return;

    onParamChange("global", {
      ...(params.global ?? {}),
      experimentType: pendingMode,
    });

    setPendingMode(null);
    setShowModeConfirm(false);
  }

  function cancelModeChange() {
    setPendingMode(null);
    setShowModeConfirm(false);
  }


  return (
    <section className="lab-leftbar">
      <div className="ll-content">
        <div className="ll-title">Configuration</div>

        {!readOnly && (
          <TemplateSection
            open={open}
            setOpen={setOpen}
            selectedTemplateId={selectedTemplateId}
            setSelectedTemplateId={setSelectedTemplateId}
            disabled={disabled}
            templates={templates}
            templatesLoading={templatesLoading}
            templatesError={templatesError}
            onApplyTemplate={onApplyTemplate}
          />
        )}

        <GlobalSettingsSection
          open={open}
          setOpen={setOpen}
          disabled={disabled}
          runMode={runMode}
          params={params}
          onModeChange={handleModeChange}
          setParam={setParam}
        />

        {PIECE_SECTIONS.map(([type, title]) => (
          <SelectedPieceSection
            key={type}
            type={type}
            title={title}
            open={open}
            setOpen={setOpen}
            pieces={Array.isArray(puzzleConfig[type]) ? puzzleConfig[type] : []}
            params={params}
            catalogLoading={catalogLoading}
            disabled={disabled}
            findPieceDef={(type, id) => findPieceDef(catalog, type, id)}
            setParam={setParam}
          />
        ))}
      </div>

      <div className="ll-actions">
        <button
          className="btn btn--green"
          type="button"
          onClick={onRun}
          disabled={ disabled}
        >
          Run
        </button>

        <button className="btn btn--red" type="button" onClick={onReset}>
          Reset
        </button>
      </div>

      {showModeConfirm && (
        <div className="mode-confirm-overlay">
          <div className="mode-confirm-modal" role="dialog" aria-modal="true">
            <h3>Switch to Runtime Study?</h3>
            <p>Switching to Runtime Study will reset your current puzzle configuration.</p>
            <p>All selected puzzle pieces and their parameter setup will be removed.</p>
            <p>Are you sure you want to continue?</p>

            <div className="mode-confirm-actions">
              <button
                type="button"
                className="btn btn--red"
                onClick={cancelModeChange}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn btn--green"
                onClick={confirmModeChange}
              >
                Yes, reset and continue
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}