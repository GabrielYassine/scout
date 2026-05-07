/**
  *
  * @author s235257 & s230632
 */

import { useEffect, useMemo, useRef, useState } from "react";

import RouteGraphModal from "@/features/run/components/charts/route/RouteGraphModal.jsx";
import SidebarSection from "../SidebarSection.jsx";
import InstanceUploadSection from "./InstanceUploadSection.jsx";
import InstanceFieldsSection from "./InstanceFieldsSection.jsx";

import "@/shared/components/styles/FormFields.css";
import "@/shared/components/styles/LabRightbar.css";

import { importInstanceFile, exportInstanceFile } from "@/shared/api/instance.js";
import { applyEditedMetadata, createEmptyTspInstance, createEmptyVrpInstance, CUSTOM_INSTANCE_NAME, getNextNodeId, } from "./instanceModel.js";
import { getInstanceViewModel, syncCitiesToTsp, syncCitiesToVrp, buildTspExportPayload, buildVrpExportPayload, } from "./instanceHelpers.js";

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
  const [open, setOpen] = useState({ description: true, instance: true, });
  const fileInputRef = useRef(null);

  const initialType = vrpInstance ? "VRP" : "TSP";
  const [instanceType, setInstanceType] = useState(initialType);

  useEffect(() => {
    if (vrpInstance) {
      setInstanceType("VRP");
    } else if (tspInstance) {
      setInstanceType("TSP");
    }
  }, [tspInstance, vrpInstance]);


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

  const view = useMemo(
    () => getInstanceViewModel({ instanceType, tspInstance, vrpInstance }),
    [instanceType, tspInstance, vrpInstance]
  );

  const dimension = view.nodes.length;

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

  const handleExport = async () => {
    setExportError(null);

    if (instanceType === "TSP" && view.nodes.length === 0) {
      setExportError("Add at least one city before exporting.");
      return;
    }

    if (instanceType === "VRP") {
      const hasDepot = view.nodes.some((node) => node.isDepot);
      const hasCustomer = view.nodes.some((node) => !node.isDepot);

      if (!hasDepot || !hasCustomer) {
        setExportError("Add at least one depot and one customer before exporting.");
        return;
      }
    }

    setExporting(true);

    try {
      const payload =
        instanceType === "VRP"  ? buildVrpExportPayload(view) : buildTspExportPayload(view);

      const requestBody = { exportType: instanceType, ...payload, };

      const content = await exportInstanceFile(requestBody);
      const extension = instanceType === "VRP" ? "vrp" : "tsp";
      const safeName = String(payload.name || "instance").trim() .replace(/[^a-zA-Z0-9-_]+/g, "_").replace(/_+/g, "_");

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

  // Switch between TSP and VRP while preserving the currently edited node positions.
  const handleTypeChange = (value) => {
    if (value === instanceType) return;

    const nodes = view.nodes;
    setInstanceType(value);

    if (value === "VRP") {
      onTspInstanceChange?.(null);
      syncCitiesToVrp(nodes, updateVrpInstance);
    } else {
      onVrpInstanceChange?.(null);

      const tspNodes = nodes.map((node) => ({...node, isDepot: false, }));

      syncCitiesToTsp(tspNodes, updateTspInstance);
    }
  };

  const handleVehicleChange = (value) => {
    if (instanceType !== "VRP") return;

    if (value === "") {
      updateVrpInstance((current) => ({ ...current, numberOfVehicles: "", }));
      return;
    }

    const parsed = Number(value);
    if (!Number.isFinite(parsed)) return;

    const nextValue = Math.max(1, Math.floor(parsed));

    updateVrpInstance((current) => ({ ...current, numberOfVehicles: nextValue, }));
  };

  const handleCapacityChange = (value) => {
    if (instanceType !== "VRP") return;

    if (value === "") {
      updateVrpInstance((current) => ({ ...current, capacity: "", }));
      return;
    }

    const parsed = Number(value);
    if (!Number.isFinite(parsed)) return;

    const nextValue = Math.max(0, parsed);

    updateVrpInstance((current) => ({ ...current, capacity: nextValue, }));
  };

  const handleCityChange = (index, field, value) => {
    const numValue = Number(value);
    if (Number.isNaN(numValue)) return;

    const updatedNodes = [...view.nodes];
    updatedNodes[index] = { ...updatedNodes[index], [field]: numValue, };

    if (instanceType === "VRP") {
      syncCitiesToVrp(updatedNodes, updateVrpInstance);
    } else {
      syncCitiesToTsp(updatedNodes, updateTspInstance);
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
      syncCitiesToVrp(nextNodes, updateVrpInstance);
    } else {
      syncCitiesToTsp(nextNodes, updateTspInstance);
    }
  };

  const handleRemoveCity = (index) => {
    const target = view.nodes[index];

    if (instanceType === "VRP" && target?.isDepot) {
      const depotCount = view.nodes.filter((node) => node.isDepot).length;
      if (depotCount <= 1) return;
    }

    const updatedNodes = view.nodes.filter((_, i) => i !== index);

    if (instanceType === "VRP") {
      syncCitiesToVrp(updatedNodes, updateVrpInstance);
    } else {
      syncCitiesToTsp(updatedNodes, updateTspInstance);
    }
  };

  // Convert node edits from the graph modal back into the correct instance structure.
  const handleCitiesUpdate = (updatedCities) => {
    const nodes = (updatedCities ?? []).map((city, index) => {
      const baseNode = view.nodes[index];

      return {
        key: baseNode.key,
        nodeId: baseNode.nodeId,
        x: city.x,
        y: city.y,
        demand: baseNode.demand ?? 0,
        isDepot: baseNode.isDepot ?? false,
      };
    });

    if (instanceType === "VRP") {
      syncCitiesToVrp(nodes, updateVrpInstance);
    } else {
      const tspNodes = nodes.map((node) => ({ ...node, isDepot: false, }));

      syncCitiesToTsp(tspNodes, updateTspInstance);
    }
  };

  const handleDepotToggle = (index) => {
    if (instanceType !== "VRP") return;

    const updatedNodes = [...view.nodes];
    const target = updatedNodes[index];

    if (!target) return;

    const depotCount = updatedNodes.filter((node) => node.isDepot).length;
    if (target.isDepot && depotCount <= 1) return;

    updatedNodes[index] = { ...target, isDepot: !target.isDepot, demand: target.isDepot ? target.demand ?? 0 : 0, };

    syncCitiesToVrp(updatedNodes, updateVrpInstance);
  };

  const depotCount = useMemo(
    () =>
      instanceType === "VRP"  ? view.nodes.filter((node) => node.isDepot).length : 0,
    [instanceType, view.nodes]
  );

  return (
    <section className="lab-rightbar">
      <div className="lr-content">
        <SidebarSection
          title="Description"
          isOpen={open.description}
          onToggle={() =>
            setOpen((current) => ({...current, description: !current.description, }))
          }
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
          onToggle={() =>
            setOpen((current) => ({ ...current, instance: !current.instance, }))
          }
        >
          <InstanceUploadSection
            fileInputRef={fileInputRef}
            uploading={uploading}
            uploadError={uploadError}
            exporting={exporting}
            exportError={exportError}
            onFileUpload={handleFileUpload}
            onExport={handleExport}
          />

          <button className="btn btn--yellow" onClick={() => setIsModalOpen(true)}>
            Edit Graph
          </button>

          <InstanceFieldsSection
            view={view}
            dimension={dimension}
            instanceType={instanceType}
            onTypeChange={handleTypeChange}
            onCapacityChange={handleCapacityChange}
            onVehicleChange={handleVehicleChange}
          />

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
          cities: view.nodes.map((node) => ({
            id: node.nodeId,
            nodeId: node.nodeId,
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