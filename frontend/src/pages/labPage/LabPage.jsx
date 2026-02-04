import "./LabPage.css";
import LabLeftbar from "../../components/LabLeftbar/LabLeftbar.jsx";
import LabRightbar from "../../components/LabRightbar.jsx";
import RepresentationSelector from "../../components/selector/representationSelector/RepresentationSelector.jsx";
import ProblemSelector from "../../components/selector/problemSelector/ProblemSelector.jsx";
import AlgorithmSelector from "../../components/selector/algorithmSelector/AlgorithmSelector.jsx";
import MutationSelector from "../../components/selector/mutationSelector/MutationSelector.jsx";
import StopConditionSelector from "../../components/selector/stopConditionSelector/StopConditionSelector.jsx";
import PopulationSelector from "../../components/selector/populationSelector/PopulationSelector.jsx";

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

  const CurrentSelector = SELECTORS[currentSelectorIndex].Component;

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
          <div className="selector-timeline">ADD SELECTOR TIMELINE</div>
          <hr className="rounded"/>
          <CurrentSelector/>
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
  );
}