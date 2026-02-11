import { useLocation, useNavigate } from "react-router-dom";
import LabLeftbar from "../components/LabLeftbar/LabLeftbar.jsx";
import RunChart from "../components/charts/RunChart.jsx";
import "./RunPage.css";

export default function RunPage({ catalog, catalogLoading, catalogError }) {
  const location = useLocation();
  const navigate = useNavigate();

  const run = location.state?.run;
  const puzzleConfig = location.state?.puzzleConfig;
  const params = location.state?.params;
  if (!run) {
      return (
        <div className="run-page">
          <LabLeftbar
            puzzleConfig={puzzleConfig ?? {}}
            params={params ?? {}}
            onParamChange={() => {}}
            onReset={() => navigate("/lab")}
            onRun={() => navigate("/lab")}
            catalog={catalog}
            catalogLoading={catalogLoading}
            catalogError={catalogError}
            readOnly
          />

          <div className="run-page-content">
            <div className="run-chart-panel">
              <div className="run-chart-title">•</div>
              <div>
                    <div>No data to plot.</div>
              </div>
            </div>
          </div>
        </div>
      );
    }

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
        <div className="run-chart-panel">
          <div className="run-chart-title">
            {run.problemId} • {run.algorithmId}
          </div>
          <RunChart run={run} />
        </div>
      </div>
    </div>
  );
}
