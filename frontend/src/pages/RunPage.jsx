import { useState, useEffect } from "react";
import { Client } from "@stomp/stompjs";
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

  const runId = location.state?.runId;

  const batchResponse = location.state?.batch;
  const isLoading = location.state?.loading;
  const error = location.state?.error;
  const puzzleConfig = location.state?.puzzleConfig ?? [];
  const params = location.state?.params ?? [];
  const initialTspInstance = location.state?.tspInstance ?? DEFAULT_TSP_INSTANCE;

  const batches = batchResponse?.batches ?? [];
  const [selectedBatchIndex, setSelectedBatchIndex] = useState(0);
  const selectedBatch = batches[selectedBatchIndex];
  const runs = selectedBatch?.runs ?? [];

  const [tspInstance] = useState(initialTspInstance);

  useEffect(() => {
    if (!runId) {
      console.warn("No runId provided; skipping websocket connection");
      return;
    }

    const wsUrl = `${window.location.protocol === "https:" ? "wss" : "ws"}://${window.location.host}/ws`;
    console.log("Connecting websocket", { runId, wsUrl });

    const client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 0,
    });

    client.debug = (...args) => console.log("WS debug:", ...args);

    client.onConnect = () => {
      console.log("WebSocket connected", runId);
      client.subscribe(`/topic/run/${runId}`, (message) => {
        const data = JSON.parse(message.body);
        console.log("Run update:", data);

        if (data.type === "RUN_FINISHED" || data.type === "RUN_DISCONNECTED") {
          client.deactivate();
        }
      });

      client.publish({
        destination: `/app/run/${runId}/connect`,
        body: JSON.stringify({}),
      });
    };

    client.onStompError = (frame) => {
      console.error("WebSocket STOMP error", frame.headers["message"], frame.body);
    };

    client.onWebSocketError = (event) => {
      console.error("WebSocket transport error", event);
    };

    client.activate();

    return () => {
      try {
        client.publish({
          destination: `/app/run/${runId}/disconnect`,
          body: JSON.stringify({}),
        });
        client.deactivate();
      } catch (e) {
        console.error("Failed to close WebSocket", e);
      }
    };
  }, [runId]);

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
       {isLoading ? (
         <div className="run-loading">
           <div className="spinner" aria-label="Loading" />
           <div>Preparing run...</div>
         </div>
       ) : error ? (
         <div className="run-chart-panel">
           <div className="run-chart-title">Run failed</div>
           <div>{error}</div>
         </div>
       ) : batches.length === 0 ? (
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
