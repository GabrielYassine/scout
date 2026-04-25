import { useMemo, useRef, useState } from "react";

import RouteGraphModal from "@/features/run/components/charts/RouteGraphModal.jsx";
import FieldRow from "./FieldRow.jsx";
import ParamField from "./ParamField.jsx";
import SidebarSection from "./SidebarSection.jsx";

import "./FormFields.css";
import "./LabRightbar.css";

import { importInstanceFile, exportInstanceFile } from "@/shared/api/instance.js";

import {
  applyEditedMetadata,
  buildVrpNodes,
  createEmptyTspInstance,
  createEmptyVrpInstance,
  CUSTOM_INSTANCE_NAME,
  EDGE_WEIGHT_TYPE,
  getNextNodeId,
} from "./instanceModel.js";

const LabRightbar = ({
  hoverInfo,
  tspInstance,
  vrpInstance,
  onTspInstanceChange,
  onVrpInstanceChange,
}) => {
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState(null);
  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [open, setOpen] = useState({
    description: true,
    instance: true,
  });
  const fileInputRef = useRef(null);

  const initialType = vrpInstance ? "VRP" : "TSP";
  const [instanceType, setInstanceType] = useState(initialType);

  const updateTspInstance = (updater) => {
    if (!onTspInstanceChange) return;
    onTspInstanceChange((prev) => {
      const base = prev ?? createEmptyTspInstance();
      const next = typeof updater === "function" ? updater(base) : updater;
      return applyEditedMetadata(next, prev);
    });
  };

  const updateVrpInstance = (updater) => {
    if (!onVrpInstanceChange) return;
    onVrpInstanceChange((prev) => {
      const base = prev ?? createEmptyVrpInstance();
      const next = typeof updater === "function" ? updater(base) : updater;
      return applyEditedMetadata(next, prev);
    });
  };

  const getViewModel = () => {
    if (instanceType === "VRP") {
      const nodes = buildVrpNodes(vrpInstance);
      const vrpVehicles = vrpInstance?.numberOfVehicles;
      return {
        name: vrpInstance?.name ?? CUSTOM_INSTANCE_NAME,
        comment: vrpInstance?.comment ?? "",
        type: "VRP",
        numberOfVehicles: vrpVehicles === "" ? "" : vrpVehicles ?? 1,
        capacity: vrpInstance?.capacity ?? "",
        nodes,
      };
    }

    return {
      name: tspInstance?.name ?? CUSTOM_INSTANCE_NAME,
      comment: tspInstance?.comment ?? "",
      type: "TSP",
      numberOfVehicles: 1,
      capacity: "",
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
      const result = await importInstanceFile({
        fileName: file.name,
        content,
      });

      const importedType = result?.instanceType;
      const importedInstance = result?.instance;

      if (!importedType || !importedInstance) {
        throw new Error("Backend returned an invalid import response");
      }

      if (importedType === "VRP") {
        setInstanceType("VRP");
        onTspInstanceChange?.(null);
        onVrpInstanceChange?.({
          ...importedInstance,
          name: importedInstance.name || file.name.replace(/\.[^/.]+$/, ""),
          source: "import",
        });
      } else {
        setInstanceType("TSP");
        onVrpInstanceChange?.(null);
        onTspInstanceChange?.({
          ...importedInstance,
          name: importedInstance.name || file.name.replace(/\.[^/.]+$/, ""),
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

  const buildTspExportPayload = () => ({
    name: view.name || CUSTOM_INSTANCE_NAME,
    comment: view.comment || "",
    cities: view.nodes.map((node, idx) => ({ id: idx, x: node.x, y: node.y })),
  });

  const buildVrpExportPayload = () => {
    const depotNodes = view.nodes.filter((n) => n.isDepot);
    const depots = depotNodes.map((d, idx) => ({
      id: idx,
      nodeId: d.nodeId ?? idx + 1,
      x: d.x,
      y: d.y,
    }));
    const customers = view.nodes
      .filter((n) => !n.isDepot)
      .map((c, idx) => ({
        id: idx,
        x: c.x,
        y: c.y,
        demand: c.demand ?? 0,
        originalId: c.nodeId ?? idx + 1,
      }));
    const primaryDepot = depots[0] ? { x: depots[0].x, y: depots[0].y } : null;

    return {
      name: view.name || CUSTOM_INSTANCE_NAME,
      comment: view.comment || "",
      type: "CVRP",
      edgeWeightType: EDGE_WEIGHT_TYPE,
      capacity: view.capacity ?? 0,
      numberOfVehicles: view.numberOfVehicles ?? 1,
      depot: primaryDepot,
      depots,
      customers,
    };
  };

  const handleExport = async () => {
    setExportError(null);

    if (instanceType === "TSP" && view.nodes.length === 0) {
      setExportError("Add at least one city before exporting.");
      return;
    }
    if (instanceType === "VRP") {
      const hasDepot = view.nodes.some((n) => n.isDepot);
      const hasCustomer = view.nodes.some((n) => !n.isDepot);
      if (!hasDepot || !hasCustomer) {
        setExportError("Add at least one depot and one customer before exporting.");
        return;
      }
    }

    setExporting(true);
    try {
      const payload = instanceType === "VRP" ? buildVrpExportPayload() : buildTspExportPayload();
      const requestBody = {
        exportType: instanceType,
        ...payload,
      };

      const content = await exportInstanceFile(requestBody);
      const extension = instanceType === "VRP" ? "vrp" : "tsp";
      const safeName = String(payload.name || "instance").trim().replace(/[^a-zA-Z0-9-_]+/g, "_").replace(/_+/g, "_");
      const fileName = `${safeName || "instance"}.${extension}`;

      const blob = new Blob([content], { type: "text/plain" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      setExportError(err.message || "Failed to export instance");
      console.error("Instance export error:", err);
    } finally {
      setExporting(false);
    }
  };

  const handleTypeChange = (value) => {
    if (value === instanceType) return;

    const nodes = view.nodes;
    setInstanceType(value);

    if (value === "VRP") {
      onTspInstanceChange?.(null);
      syncCitiesToVrp(nodes);
    } else {
      onVrpInstanceChange?.(null);
      syncCitiesToTsp(nodes);
    }
  };

  const handleVehicleChange = (value) => {
    if (instanceType !== "VRP") return;
    if (value === "") {
      updateVrpInstance((current) => ({
        ...current,
        numberOfVehicles: "",
      }));
      return;
    }

    const parsed = Number(value);
    if (!Number.isFinite(parsed)) return;
    const nextValue = Math.max(1, Math.floor(parsed));

    updateVrpInstance((current) => ({
      ...current,
      numberOfVehicles: nextValue,
    }));
  };

  const handleCapacityChange = (value) => {
    if (instanceType !== "VRP") return;
    if (value === "") {
      updateVrpInstance((current) => ({
        ...current,
        capacity: "",
      }));
      return;
    }

    const parsed = Number(value);
    if (!Number.isFinite(parsed)) return;
    const nextValue = Math.max(0, parsed);

    updateVrpInstance((current) => ({
      ...current,
      capacity: nextValue,
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

  const depotCount = useMemo(
    () => (instanceType === "VRP" ? view.nodes.filter((n) => n.isDepot).length : 0),
    [instanceType, view.nodes]
  );

  return (
    <section className="lab-rightbar">
      <div className="lr-content">
        <SidebarSection
          title="Description"
          isOpen={open.description}
          onToggle={() => setOpen((o) => ({ ...o, description: !o.description }))}
        >
          <div className="lr-description-body">
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
        </SidebarSection>

        <SidebarSection
          title="Problem Instance"
          isOpen={open.instance}
          onToggle={() => setOpen((o) => ({ ...o, instance: !o.instance }))}
        >
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
            <button
              className={`tsp-upload-btn tsp-export-btn ${exporting ? "disabled" : ""}`}
              type="button"
              onClick={handleExport}
              disabled={exporting}
            >
              {exporting ? "Exporting..." : "Export Instance File"}
            </button>
            <div className="tsp-file-hint">Accepts .tsp and .vrp file formats</div>
            {uploadError && <div className="tsp-upload-error">{uploadError}</div>}
            {exportError && <div className="tsp-upload-error">{exportError}</div>}
          </div>

          <button className="view-graph-btn" onClick={() => setIsModalOpen(true)}>
            Edit Graph
          </button>

          <div className="instance-fields">
            <span className="instance-summary-label">Name:</span>
            <span className="instance-summary-value">
              {view.name || CUSTOM_INSTANCE_NAME}
            </span>

            <span className="instance-summary-label">Comment:</span>
            <span className="instance-summary-value">
              {view.comment || "No comment"}
            </span>

            <span className="instance-summary-label">Dimension:</span>
            <span className="instance-summary-value">{dimension}</span>

            <FieldRow label="Instance Type">
              <select
                className="field-input"
                value={instanceType}
                onChange={(e) => handleTypeChange(e.target.value)}
              >
                <option value="TSP">TSP</option>
                <option value="VRP">VRP</option>
              </select>
            </FieldRow>

            <ParamField
              def={{
                key: "capacity",
                label: "Capacity",
                type: "double",
                min: 0,
                defaultValue: 0,
              }}
              value={view.capacity}
              disabled={instanceType === "TSP"}
              onValueChange={(v) => handleCapacityChange(v)}
            />

            <ParamField
              def={{
                key: "numberOfVehicles",
                label: "Vehicle Amount",
                type: "int",
                min: 1,
                defaultValue: 1,
              }}
              value={view.numberOfVehicles}
              disabled={instanceType === "TSP"}
              onValueChange={(v) => handleVehicleChange(v)}
            />

            <FieldRow label="Edge Weight Type">
              <input className="field-input" value={EDGE_WEIGHT_TYPE} readOnly disabled />
            </FieldRow>
          </div>

          {instanceType === "VRP" && depotCount <= 1 && (
            <div className="tsp-file-hint">VRP needs at least one depot.</div>
          )}
        </SidebarSection>
      </div>

      <RouteGraphModal
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
        nodes={view.nodes}
        instanceType={instanceType}
        onCityChange={handleCityChange}
        onAddCity={handleAddCity}
        onRemoveCity={handleRemoveCity}
        onDepotToggle={handleDepotToggle}
      />
    </section>
  );
};

export default LabRightbar;