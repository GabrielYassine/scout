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
    return { ...next, name: CUSTOM_INSTANCE_NAME, source: "custom" };
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

export const buildVrpNodes = (vrp) => {
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
  return [...depots, ...customers].sort((a, b) => (a.nodeId ?? 0) - (b.nodeId ?? 0));
};

export const getNextNodeId = (nodes) => {
  if (!nodes.length) return 1;
  const maxId = Math.max(...nodes.map((n) => Number(n.nodeId) || 0));
  return maxId + 1;
};
