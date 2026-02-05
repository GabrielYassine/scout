import "./LabPage.css";
import LabLeftbar from "../../components/LabLeftbar/LabLeftbar.jsx";
import LabRightbar from "../../components/LabRightbar.jsx";
import RunConfigPuzzle from "../../components/runConfigPuzzle/RunConfigPuzzle.jsx";
import RepresentationSelector from "../../components/selector/representationSelector/RepresentationSelector.jsx";
import ProblemSelector from "../../components/selector/problemSelector/ProblemSelector.jsx";
import AlgorithmSelector from "../../components/selector/algorithmSelector/AlgorithmSelector.jsx";
import MutationSelector from "../../components/selector/mutationSelector/MutationSelector.jsx";
import StopConditionSelector from "../../components/selector/stopConditionSelector/StopConditionSelector.jsx";
import PopulationSelector from "../../components/selector/populationSelector/PopulationSelector.jsx";

import { useState } from "react";
import { DndContext, DragOverlay, rectIntersection } from "@dnd-kit/core";
import { useSessionStorageState } from "../../hooks/useSessionStorageState.js";

const DEFAULT_FORM = {
  problem: "onemax",
  algorithm: "1p1-ea",
  problemParams: {},
  algorithmParams: {},
};

const SELECTORS = [
  { id: "representation", Component: RepresentationSelector },
  { id: "problem", Component: ProblemSelector },
  { id: "algorithm", Component: AlgorithmSelector },
  { id: "mutation", Component: MutationSelector },
  { id: "stopCondition", Component: StopConditionSelector },
  { id: "population", Component: PopulationSelector },
];

export default function LabPage({catalog, catalogLoading, catalogError}) {
  const [form, setForm, resetForm] = useSessionStorageState(
    "scout:labForm",
    DEFAULT_FORM
  );
  const [currentSelectorIndex, setCurrentSelectorIndex] = useSessionStorageState(
    "scout:currentSelector",
    0
  );

  // State for the puzzle configuration
  const [puzzleConfig, setPuzzleConfig] = useState({
    searchSpace: null,
    problem: null,
    algorithm: null,
    mutation: null,
    acceptance: null,
    stopCondition: null,
  });

  const [activeId, setActiveId] = useState(null);
  const [activeLabel, setActiveLabel] = useState(null);

  const CurrentSelector = SELECTORS[currentSelectorIndex].Component;

  function handleDragStart(event) {
    const { active } = event;
    setActiveId(active.id);
    setActiveLabel(active.data?.current?.label || active.id);
  }

  function handleDragEnd(event) {
    const { active, over } = event;
    setActiveId(null);
    setActiveLabel(null);

    // If dragging a piece from a drop zone
    if (active.id.toString().startsWith('dropped-')) {
      const fromZone = active.data?.current?.fromZone;
      const originalId = active.data?.current?.originalId;

      if (!over) {
        // Dragged outside - remove from zone
        setPuzzleConfig(prev => ({
          ...prev,
          [fromZone]: null,
        }));
        return;
      }

      // Dropped into another zone
      if (over.id !== fromZone) {
        setPuzzleConfig(prev => ({
          ...prev,
          [fromZone]: null,
          [over.id]: {
            id: originalId,
            label: active.data?.current?.label || originalId,
          }
        }));
      }
      return;
    }

    // Dragging a new piece from selector
    if (!over) return;

    // Update the puzzle configuration based on drop zone
    setPuzzleConfig(prev => ({
      ...prev,
      [over.id]: {
        id: active.id,
        label: active.data?.current?.label || active.id,
      }
    }));
  }

  function handlePrevious() {
    setCurrentSelectorIndex((prev) => Math.max(0, prev - 1));
  }

  function handleNext() {
    setCurrentSelectorIndex((prev) => Math.min(SELECTORS.length - 1, prev + 1));
  }

  async function onRun() {
    const res = await fetch("/api/run", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        problemId: form.problem,
        problemParams: form.problemParams,
        algorithmId: form.algorithm,
        algorithmParams: form.algorithmParams,
      }),
    });

    const result = await res.json();
    console.log(result);
  }

  return (
    <DndContext
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
      collisionDetection={rectIntersection}
    >
      <div className="lab-page">
        <LabLeftbar
            form={form}
            onChange={setForm}
            onReset={resetForm}
            onRun={onRun}
            catalog={catalog}
            catalogLoading={catalogLoading}
            catalogError={catalogError}
        />

        <div className="lab-page-content">
          <div className="selector-timeline">
            <RunConfigPuzzle config={puzzleConfig} />
          </div>
          <hr className="rounded"/>
          <CurrentSelector catalog={catalog} catalogLoading={catalogLoading} catalogError={catalogError} />
          <div className="navigation-buttons">
            <button className="btn btn--red" type="button"
                onClick={handlePrevious}
                disabled={currentSelectorIndex === 0}
            >
              Previous
            </button>
            <button className="btn btn--green" type="button"
                onClick={handleNext}
                disabled={currentSelectorIndex === SELECTORS.length - 1}
            >
              Next
            </button>
          </div>
        </div>
        <LabRightbar/>
      </div>
      <DragOverlay>
        {activeId ? (
          <div className="puzzle-piece" style={{
            cursor: 'grabbing',
            boxShadow: '0 8px 24px rgba(0, 0, 0, 0.3)',
          }}>
            <div className="puzzle-piece-title">{activeLabel}</div>
          </div>
        ) : null}
      </DragOverlay>
    </DndContext>
  );
}