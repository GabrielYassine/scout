import { useLocation, useNavigate } from "react-router-dom";
import LabLeftbar from "../components/LabLeftbar/LabLeftbar.jsx";
import RunChart from "../components/charts/RunChart.jsx";
import "./RunPage.css";

export default function RunPage({ catalog, catalogLoading, catalogError }) {
  const location = useLocation();
  const navigate = useNavigate();

  const batch = location.state?.batch;
  const puzzleConfig = location.state?.puzzleConfig;
  const params = location.state?.params;
  const runs = batch?.runs ?? [];


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
       {runs.length === 0 ? (
         <div className="run-chart-panel">
           <div className="run-chart-title">No run data</div>
           <div>No data to plot..</div>
         </div>
       ) : (
         <div className="run-stack">
           {runs.map((run, idx) => (
             <RunChart key={idx} run={run} runIndex={idx + 1} />
           ))}
         </div>
       )}
     </div>
   </div>
 );

}
