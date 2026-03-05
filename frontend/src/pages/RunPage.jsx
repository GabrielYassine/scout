import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import LabLeftbar from "../components/LabLeftbar/LabLeftbar.jsx";
import LabRightbar from "../components/LabRightbar.jsx";
import RunChart from "../components/charts/RunChart.jsx";
import { DEFAULT_TSP_INSTANCE } from "../contexts/PuzzleConfigContext.jsx";
import "./RunPage.css";
import "../components/LabLeftbar/LabLeftbar.css";
import "../components/LabRightbar.css";

export default function RunPage({ catalog, catalogLoading, catalogError }) {
  const location = useLocation();
  const navigate = useNavigate();

  const batchResponse = location.state?.batch;
  const puzzleConfig = location.state?.puzzleConfig;
  const params = location.state?.params;
  const initialTspInstance = location.state?.tspInstance;

  const batches = batchResponse?.batches ?? [];
  const [selectedBatchIndex, setSelectedBatchIndex] = useState(0);
  const selectedBatch = batches[selectedBatchIndex];
  const runs = selectedBatch?.runs ?? [];

  const [tspInstance] = useState(initialTspInstance || DEFAULT_TSP_INSTANCE);

  return (
    <div className="run-page">
      <LabLeftbar
        puzzleConfig={puzzleConfig}
        params={params}
        onParamChange={() => {}}
        onReset={() => navigate("/lab")}
        onRun={() => navigate("/lab")}
        catalog={catalog}
        catalogLoading={catalogLoading}
        catalogError={catalogError}
        readOnly
      />

     <div className="run-page-content">
       {batches.length === 0 ? (
         <div className="run-chart-panel">
           <div className="run-chart-title">No run data</div>
           <div>No data to plot..</div>
         </div>
       ) : (
         <>
           {batches.length > 1 && (
             <div className="run-selector">
               <label htmlFor="batch-select" className="form-label">
                 Select Run:
               </label>
               <select
                 id="batch-select"
                 className="form-select"
                 value={selectedBatchIndex}
                 onChange={(e) => setSelectedBatchIndex(Number(e.target.value))}
               >
                 {batches.map((batch, idx) => (
                   <option key={idx} value={idx}>
                     Run {batch.runIndex} (Seed: {batch.seed})
                   </option>
                 ))}
               </select>
             </div>
           )}

           <div className="run-stack">
             {runs.map((run, idx) => (
               <RunChart
                 key={`${selectedBatchIndex}-${idx}`}
                 run={run}
                 runIndex={selectedBatch?.runIndex ?? selectedBatchIndex + 1}
                 problemIndex={idx + 1}
               />
             ))}
           </div>
         </>
       )}
     </div>
     <LabRightbar
       hoverInfo={null}
       tspInstance={tspInstance}
       onTspInstanceChange={() => {}}
     />
   </div>
 );

}
