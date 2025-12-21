import "./LabPage.css";
import LabLeftbar from "../components/LabLeftbar.jsx";
import LabRightbar from "../components/LabRightbar.jsx";
import { useSessionStorageState } from "../hooks/useSessionStorageState.js";

const DEFAULT_FORM = {
  problem: "onemax",
  algorithm: "1p1-ea",
  budget: 10000,
};

export default function LabPage() {
  const [form, setForm, resetForm] = useSessionStorageState(
    "scout:labForm",
    DEFAULT_FORM
  );

  return (
    <div className="lab-page">
      <LabLeftbar form={form} onChange={setForm} onReset={resetForm} />

      <div className="lab-page-content"></div>

      <LabRightbar form={form} />
    </div>
  );
}
