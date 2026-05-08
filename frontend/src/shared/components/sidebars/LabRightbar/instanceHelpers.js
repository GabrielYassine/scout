/**
 * Helpers for instance view models and export payloads.
 * @author s235257
 */

import {
  buildVrpNodes,
  CUSTOM_INSTANCE_NAME,
  EDGE_WEIGHT_TYPE,
} from "./instanceModel.js";

export function getInstanceViewModel({ instanceType, tspInstance, vrpInstance }) {
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
    nodes: (tspInstance?.cities ?? []).map((city) => ({
      key: `city-${city.nodeId}`,
      nodeId: city.nodeId,
      x: city.x,
      y: city.y,
      demand: 0,
      isDepot: false,
    })),
  };
}

// Separates the editable node list into the VRP structures expected by the backend.
function splitVrpNodes(nodes) {
  const safeNodes = Array.isArray(nodes) ? nodes : [];

  const depotNodes = safeNodes.filter((node) => node.isDepot);
  const depotIdSet = new Set(depotNodes.map((depot) => depot.nodeId));

  const customerNodes = safeNodes.filter(
    (node) => !node.isDepot && !depotIdSet.has(node.nodeId)
  );

  const depots = depotNodes.map((depot) => ({
    id: depot.nodeId,
    nodeId: depot.nodeId,
    x: depot.x,
    y: depot.y,
  }));

  const customers = customerNodes.map((customer) => ({
    id: customer.nodeId,
    nodeId: customer.nodeId,
    x: customer.x,
    y: customer.y,
    demand: customer.demand ?? 0,
    originalId: customer.nodeId,
  }));

  // The current backend model supports one primary depot, while keeping all depot nodes for export/import.
  const primaryDepot = depots[0]
    ? {
        id: depots[0].nodeId,
        nodeId: depots[0].nodeId,
        x: depots[0].x,
        y: depots[0].y,
      }
    : null;

  return {
    depots,
    customers,
    primaryDepot,
  };
}

export function syncCitiesToTsp(nodes, updateTspInstance) {
  const safeNodes = Array.isArray(nodes) ? nodes : [];

  const normalized = safeNodes.map((node) => ({
    id: node.nodeId,
    nodeId: node.nodeId,
    x: node.x,
    y: node.y,
  }));

  updateTspInstance((current) => ({
    ...current,
    type: "TSP",
    edgeWeightType: EDGE_WEIGHT_TYPE,
    dimension: normalized.length,
    cities: normalized,
  }));
}

export function syncCitiesToVrp(nodes, updateVrpInstance) {
  updateVrpInstance((current) => {
    const { depots, customers, primaryDepot } = splitVrpNodes(nodes);

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
}

export function buildTspExportPayload(view) {
  return {
    name: view.name || CUSTOM_INSTANCE_NAME,
    comment: view.comment || "",
    cities: view.nodes.map((node) => ({
      id: node.nodeId,
      nodeId: node.nodeId,
      x: node.x,
      y: node.y,
    })),
  };
}

export function buildVrpExportPayload(view) {
  const { depots, customers, primaryDepot } = splitVrpNodes(view.nodes);

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
}