/**
 * Model helpers for TSP/VRP instance metadata.
 * @author s235257
 */

const CUSTOM_INSTANCE_NAME = "Custom Instance";

export const EDGE_WEIGHT_TYPE = "EUC_2D";
export const CUSTOM_EDITED_COMMENT = "Custom edited instance";
export { CUSTOM_INSTANCE_NAME };

export const createEmptyTspInstance = () => ({
  name: CUSTOM_INSTANCE_NAME,
  comment: "",
  type: "TSP",
  dimension: 0,
  edgeWeightType: EDGE_WEIGHT_TYPE,
  cities: [],
  source: "manual",
});

export const createEmptyVrpInstance = () => ({
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

export const markCustomImportIfNeeded = (next, prev) => {
  if (prev?.source === "import") {
    return {
      ...next,
      name: CUSTOM_INSTANCE_NAME,
      source: "custom",
    };
  }

  return next;
};

export const applyEditedMetadata = (next, prev) => {
  const updated = markCustomImportIfNeeded(next, prev);

  return {
    ...updated,
    comment: CUSTOM_EDITED_COMMENT,
    source: "custom",
  };
};

// Normalizes depot data so imported and manually created VRP instances use the same node format.
const buildDepotList = (vrp) => {
  if (Array.isArray(vrp?.depots)) {
    return vrp.depots.map((depot, index) => ({
      id: index,
      nodeId: depot.nodeId ?? depot.originalId ?? index,
      x: depot.x,
      y: depot.y,
    }));
  }

  if (vrp?.depot) {
    return [
      {
        id: 0,
        nodeId: 1,
        x: vrp.depot.x,
        y: vrp.depot.y,
      },
    ];
  }

  return [];
};

export const buildVrpNodes = (vrp) => {
  const depots = buildDepotList(vrp).map((depot) => ({
    key: `depot-${depot.nodeId}`,
    nodeId: depot.nodeId,
    x: depot.x,
    y: depot.y,
    demand: 0,
    isDepot: true,
  }));

  const depotIds = new Set(depots.map((depot) => depot.nodeId));

  // Customers with the same node id as a depot are skipped to avoid duplicate editable nodes.
  const customers = (vrp?.customers ?? [])
    .map((customer, index) => ({
      key: `cust-${customer.originalId ?? index}`,
      nodeId: customer.originalId ?? index + 1,
      x: customer.x,
      y: customer.y,
      demand: customer.demand ?? 0,
      isDepot: false,
    }))
    .filter((customer) => !depotIds.has(customer.nodeId));

  return [...depots, ...customers].sort(
    (a, b) => (a.nodeId ?? 0) - (b.nodeId ?? 0)
  );
};

export const getNextNodeId = (nodes) => {
  if (!nodes.length) return 1;

  const maxId = Math.max(...nodes.map((node) => Number(node.nodeId) || 0));
  return maxId + 1;
};