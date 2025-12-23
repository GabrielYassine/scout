import "./LabPage.css";
import LabLeftbar from "../components/LabLeftbar/LabLeftbar.jsx";
import LabRightbar from "../components/LabRightbar.jsx";
import { useSessionStorageState } from "../hooks/useSessionStorageState.js";


// Default form values
const DEFAULT_FORM = {
  problem: "onemax",
  algorithm: "1p1-ea",
  problemParams: {},
  algorithmParams: {},
};


export default function LabPage({catalog, catalogLoading, catalogError}) {
  const [form, setForm, resetForm] = useSessionStorageState(
    "scout:labForm",
    DEFAULT_FORM
  );

  return (
    <div className="lab-page">
      <LabLeftbar
        form={form}
        onChange={setForm}
        onReset={resetForm}
        catalog={catalog}
        catalogLoading={catalogLoading}
        catalogError={catalogError}
      />

      <div className="lab-page-content"></div>
      <LabRightbar/>
    </div>
  );
}
