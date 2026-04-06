import { useState, useRef } from "react";
import "./LabRightbar.css";
import TSPGraphModal from "../Charts/TSPGraphModal.jsx";
import FieldRow from "./FieldRow.jsx";
import { detectInstanceType, parseTspContent, parseVrpContent } from "./instanceParsing.js";

const CUSTOM_INSTANCE_NAME = "Custom Instance";
const EDGE_WEIGHT_TYPE = "EUC_2D";

const createEmptyTspInstance = () => ({
  name: CUSTOM_INSTANCE_NAME,
  comment: "",
  type: "TSP",
  dimension: 0,
  edgeWeightType: EDGE_WEIGHT_TYPE,
  cities: [],
  source: "manual",
});

const createEmptyVrpInstance = () => ({
  name: CUSTOM_INSTANCE_NAME,
  comment: "",
  type: "CVRP",
  dimension: 0,
  edgeWeightType: EDGE_WEIGHT_TYPE,
  capacity: "",
  numberOfVehicles: "",
  depot: { x: 0, y: 0 },
  depots: [{ id: 0, x: 0, y: 0 }],
  customers: [],
  source: "manual",
});

const markCustomImportIfNeeded = (next, prev) => {
  if (prev?.source === "import") {
    return { ...next, name: CUSTOM_INSTANCE_NAME, source: "custom" };
  }
  return next;
};

const buildDepotList = (vrp) => {
  if (Array.isArray(vrp?.depots)) {
    return vrp.depots.map((d, idx) => ({
      id: idx,
      nodeId: d.nodeId ?? d.originalId ?? idx,
      x: d.x,
      y: d.y,
    }));
  }
  if (vrp?.depot) {
    return [{ id: 0, nodeId: 1, x: vrp.depot.x, y: vrp.depot.y }];
  }
  return [];
};

const buildVrpNodes = (vrp) => {
  const depots = buildDepotList(vrp).map((d) => ({
    key: `depot-${d.nodeId}`,
    nodeId: d.nodeId,
    x: d.x,
    y: d.y,
    demand: 0,
    isDepot: true,
  }));
  const depotIds = new Set(depots.map((d) => d.nodeId));
  const customers = (vrp?.customers ?? [])
    .map((c, idx) => ({
      key: `cust-${c.originalId ?? idx}`,
      nodeId: c.originalId ?? idx + 1,
      x: c.x,
      y: c.y,
      demand: c.demand ?? 0,
      isDepot: false,
    }))
    .filter((c) => !depotIds.has(c.nodeId));
  return [...depots, ...customers];
};

const getNextNodeId = (nodes) => {
  if (!nodes.length) return 1;
  const maxId = Math.max(...nodes.map((n) => Number(n.nodeId) || 0));
  return maxId + 1;
};

export default function LabRightbar({
  hoverInfo,
  tspInstance,
  vrpInstance,
  onTspInstanceChange,
  onVrpInstanceChange,
}) {
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const fileInputRef = useRef(null);

  const initialType = vrpInstance ? "VRP" : "TSP";
  const [instanceType, setInstanceType] = useState(initialType);

  const updateTspInstance = (updater) => {
    if (!onTspInstanceChange) return;
    onTspInstanceChange((prev) => {
      const base = prev ?? createEmptyTspInstance();
      const next = typeof updater === "function" ? updater(base) : updater;
      return markCustomImportIfNeeded(next, prev);
    });
  };

  const updateVrpInstance = (updater) => {
    if (!onVrpInstanceChange) return;
    onVrpInstanceChange((prev) => {
      const base = prev ?? createEmptyVrpInstance();
      const next = typeof updater === "function" ? updater(base) : updater;
      return markCustomImportIfNeeded(next, prev);
    });
  };

  const getViewModel = () => {
    if (instanceType === "VRP") {
      const nodes = buildVrpNodes(vrpInstance);
      return {
        name: vrpInstance?.name ?? CUSTOM_INSTANCE_NAME,
        comment: vrpInstance?.comment ?? "",
        type: "VRP",
        numberOfVehicles: vrpInstance?.numberOfVehicles ?? 1,
        nodes,
      };
    }

    return {
      name: tspInstance?.name ?? CUSTOM_INSTANCE_NAME,
      comment: tspInstance?.comment ?? "",
      type: "TSP",
      numberOfVehicles: 1,
      nodes: (tspInstance?.cities ?? []).map((c, idx) => ({
        key: `city-${idx}`,
        nodeId: idx,
        x: c.x,
        y: c.y,
        demand: 0,
        isDepot: false,
      })),
    };
  };

  const view = getViewModel();
  const dimension = view.nodes.length;

  const syncCitiesToTsp = (nodes) => {
    const normalized = nodes.map((node, idx) => ({ id: idx, x: node.x, y: node.y }));
    updateTspInstance((current) => ({
      ...current,
      type: "TSP",
      edgeWeightType: EDGE_WEIGHT_TYPE,
      dimension: normalized.length,
      cities: normalized,
    }));
  };

  const syncCitiesToVrp = (nodes) => {
    updateVrpInstance((current) => {
      const depotNodes = nodes.filter((n) => n.isDepot);
      const depotIdSet = new Set(depotNodes.map((d) => d.nodeId));
      const customerNodes = nodes.filter((n) => !n.isDepot && !depotIdSet.has(n.nodeId));
      const depots = depotNodes.map((d, idx) => ({
        id: idx,
        nodeId: d.nodeId ?? idx + 1,
        x: d.x,
        y: d.y,
      }));
      const customers = customerNodes.map((c, idx) => ({
        id: idx,
        x: c.x,
        y: c.y,
        demand: c.demand ?? 0,
        originalId: c.nodeId ?? idx + 1,
      }));
      const primaryDepot = depots[0] ? { x: depots[0].x, y: depots[0].y } : null;

      return {
        ...current,
        type: "CVRP",
        edgeWeightType: EDGE_WEIGHT_TYPE,
        dimension: customers.length + depots.length,
        customers,
        depot: primaryDepot,
        depots,
      };
    });
  };

  const handleFileUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploading(true);
    setUploadError(null);

    try {
      const content = await file.text();
      const detectedType = detectInstanceType(file.name, content);

      if (detectedType === "VRP") {
        const parsed = parseVrpContent(content);
        const name = parsed.name || file.name.replace(/\.[^/.]+$/, "");
        setInstanceType("VRP");
        onVrpInstanceChange?.({
          ...parsed,
          name: name || CUSTOM_INSTANCE_NAME,
          source: "import",
        });
      } else {
        const parsed = parseTspContent(content);
        const name = parsed.name || file.name.replace(/\.[^/.]+$/, "");
        setInstanceType("TSP");
        onTspInstanceChange?.({
          ...parsed,
          name: name || CUSTOM_INSTANCE_NAME,
          source: "import",
        });
      }
    } catch (err) {
      setUploadError(err.message || "Failed to import file");
      console.error("Instance import error:", err);
    } finally {
      setUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  };

  const handleTypeChange = (value) => {
    if (value === instanceType) return;
    const nodes = view.nodes;

    setInstanceType(value);
    if (value === "VRP") {
      syncCitiesToVrp(nodes);
    } else {
      syncCitiesToTsp(nodes);
    }
  };

  const handleVehicleChange = (value) => {
    if (instanceType !== "VRP") return;
    const parsed = Number(value);
    if (!Number.isFinite(parsed)) return;
    const nextValue = Math.max(1, Math.floor(parsed));

    updateVrpInstance((current) => ({
      ...current,
      numberOfVehicles: nextValue,
    }));
  };

  const handleCityChange = (index, field, value) => {
    const numValue = Number(value);
    if (Number.isNaN(numValue)) return;

    const updatedNodes = [...view.nodes];
    updatedNodes[index] = {
      ...updatedNodes[index],
      [field]: numValue,
    };

    if (instanceType === "VRP") {
      syncCitiesToVrp(updatedNodes);
    } else {
      syncCitiesToTsp(updatedNodes);
    }
  };

  const handleAddCity = () => {
    const nextNodeId = getNextNodeId(view.nodes);
    const nextNodes = [
      ...view.nodes,
      {
        key: `node-${nextNodeId}`,
        nodeId: nextNodeId,
        x: 0,
        y: 0,
        demand: 0,
        isDepot: false,
      },
    ];
    if (instanceType === "VRP") {
      syncCitiesToVrp(nextNodes);
    } else {
      syncCitiesToTsp(nextNodes);
    }
  };

  const handleRemoveCity = (index) => {
    const target = view.nodes[index];
    if (instanceType === "VRP" && target?.isDepot) {
      const depotCount = view.nodes.filter((n) => n.isDepot).length;
      if (depotCount <= 1) return;
    }
    const updatedNodes = view.nodes.filter((_, i) => i !== index);

    if (instanceType === "VRP") {
      syncCitiesToVrp(updatedNodes);
    } else {
      const reindexed = updatedNodes.map((node, idx) => ({ ...node, nodeId: idx }));
      syncCitiesToTsp(reindexed);
    }
  };

  const handleCitiesUpdate = (updatedCities) => {
    const nodes = (updatedCities ?? []).map((city, idx) => {
      const baseNode = view.nodes[idx] ?? {};
      return {
        key: baseNode.key ?? `city-${idx}`,
        nodeId: baseNode.nodeId ?? idx,
        x: city.x,
        y: city.y,
        demand: baseNode.demand ?? 0,
        isDepot: baseNode.isDepot ?? false,
      };
    });

    if (instanceType === "VRP") {
      syncCitiesToVrp(nodes);
    } else {
      const tspNodes = nodes.map((node, idx) => ({
        ...node,
        nodeId: idx,
        isDepot: false,
      }));
      syncCitiesToTsp(tspNodes);
    }
  };

  const handleDepotToggle = (index) => {
    if (instanceType !== "VRP") return;
    const updatedNodes = [...view.nodes];
    const target = updatedNodes[index];
    if (!target) return;
    const depotCount = updatedNodes.filter((n) => n.isDepot).length;
    if (target.isDepot && depotCount <= 1) return;
    updatedNodes[index] = {
      ...target,
      isDepot: !target.isDepot,
      demand: target.isDepot ? target.demand ?? 0 : 0,
    };
    syncCitiesToVrp(updatedNodes);
  };

  return (
    <section className="lab-rightbar">
      <div className="lr-content">
        <div className="lr-title">Info</div>

        <div className="lr-section">
          <div className="lr-section-header">
            <div className="lr-section-title">Description</div>
          </div>

          <div className="lr-section-body lr-description-body">
            {hoverInfo ? (
              <>
                <div className="lr-selected-piece">{hoverInfo.title}</div>
                <div className="lr-text">{hoverInfo.description}</div>
              </>
            ) : (
              <div className="lr-placeholder">
                Hover over a puzzle piece to see its description.
              </div>
            )}
          </div>
        </div>

        <div className="lr-section">
          <div className="lr-section-header">
            <div className="lr-section-title">Problem Instance</div>
          </div>

          <div className="lr-section-body">
            <div className="tsp-upload-section">
              <input
                ref={fileInputRef}
                type="file"
                accept=".tsp,.vrp,.txt"
                onChange={handleFileUpload}
                disabled={uploading}
                className="tsp-file-input"
                id="instance-file-upload"
              />
              <label
                htmlFor="instance-file-upload"
                className={`tsp-upload-btn ${uploading ? "disabled" : ""}`}
              >
                {uploading ? "Uploading..." : "Upload Instance File"}
              </label>
              <div className="tsp-file-hint">Accepts .tsp and .vrp file formats</div>
              {uploadError && <div className="tsp-upload-error">{uploadError}</div>}
            </div>

            <div className="instance-fields">
              <FieldRow label="Name">
                <input
                  className="field-input"
                  value={view.name}
                  readOnly
                  disabled
                  placeholder={CUSTOM_INSTANCE_NAME}
                />
              </FieldRow>
              <FieldRow label="Description">
                <input
                  className="field-input"
                  value={view.comment}
                  readOnly
                  disabled
                  placeholder="Optional"
                />
              </FieldRow>
              <FieldRow label="Type">
                <select
                  className="field-input"
                  value={instanceType}
                  onChange={(e) => handleTypeChange(e.target.value)}
                >
                  <option value="TSP">TSP</option>
                  <option value="VRP">VRP</option>
                </select>
              </FieldRow>
              <FieldRow label="Dimension">
                <input
                  className="field-input"
                  type="number"
                  value={dimension}
                  readOnly
                  disabled
                />
              </FieldRow>
              <FieldRow label="Edge Weight Type">
                <input
                  className="field-input"
                  value={EDGE_WEIGHT_TYPE}
                  readOnly
                  disabled
                />
              </FieldRow>
              <FieldRow label="Vehicle Amount">
                <input
                  className="field-input"
                  type="number"
                  value={view.numberOfVehicles}
                  onChange={(e) => handleVehicleChange(e.target.value)}
                  disabled={instanceType === "TSP"}
                />
              </FieldRow>
            </div>

            {view.name && (
              <div className="tsp-instance-name">
                Instance: <strong>{view.name}</strong>
              </div>
            )}

            {view.nodes.length > 0 && (
              <button className="view-graph-btn" onClick={() => setIsModalOpen(true)}>
                View Graph
              </button>
            )}

            <div className="cities-list">
              <div className="cities-header">
                <span className="city-col-action"></span>
                <span className="city-col-id">ID</span>
                <span className="city-col-coord">X</span>
                <span className="city-col-coord">Y</span>
                <span className="city-col-action"></span>
              </div>

              <div className="cities-scroll">
                {view.nodes.map((node, index) => (
                  <div key={node.key} className="city-row">
                    <button
                      type="button"
                      className={`depot-toggle ${node.isDepot ? "active" : ""}`}
                      onClick={() => handleDepotToggle(index)}
                      title={node.isDepot ? "Depot" : "Customer"}
                      disabled={instanceType !== "VRP"}
                    />
                    <span className="city-col-id">{node.nodeId}</span>
                    <input
                      type="number"
                      className="city-input"
                      value={node.x}
                      onChange={(e) => handleCityChange(index, "x", e.target.value)}
                    />
                    <input
                      type="number"
                      className="city-input"
                      value={node.y}
                      onChange={(e) => handleCityChange(index, "y", e.target.value)}
                    />
                    <button
                      className="city-remove-btn"
                      onClick={() => handleRemoveCity(index)}
                      title="Remove city"
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>

              <button className="add-city-btn" onClick={handleAddCity}>
                + Add City
              </button>
            </div>
          </div>
        </div>
      </div>

      <TSPGraphModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        tspInstance={{
          name: view.name,
          cities: view.nodes.map((node, idx) => ({
            id: idx,
            x: node.x,
            y: node.y,
            isDepot: node.isDepot,
          })),
        }}
        onCitiesUpdate={handleCitiesUpdate}
      />
    </section>
  );
}

