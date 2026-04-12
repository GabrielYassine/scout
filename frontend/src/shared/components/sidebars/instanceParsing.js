const EDGE_WEIGHT_TYPE = "EUC_2D";

export const parseHeaderLine = (line) => {
  const match = line.match(/^([A-Z_]+)\s*:?\s*(.*)$/i);
  if (!match) return null;
  return { key: match[1].toUpperCase(), value: match[2]?.trim() ?? "" };
};

export const detectInstanceType = (fileName, content) => {
  const lower = (fileName ?? "").toLowerCase();
  if (lower.endsWith(".vrp")) return "VRP";
  if (lower.endsWith(".tsp")) return "TSP";
  if (/DEMAND_SECTION|DEPOT_SECTION/i.test(content)) return "VRP";
  return "TSP";
};

export const parseTspContent = (content) => {
  const lines = content.split(/\r?\n/);
  const headers = {};
  const cities = [];
  let inNodes = false;

  for (const rawLine of lines) {
    const line = rawLine.trim();
    if (!line) continue;
    if (/^EOF$/i.test(line)) break;

    if (/^NODE_COORD_SECTION$/i.test(line)) {
      inNodes = true;
      continue;
    }

    if (!inNodes) {
      const parsed = parseHeaderLine(line);
      if (parsed) headers[parsed.key] = parsed.value;
      continue;
    }

    const parts = line.split(/\s+/);
    if (parts.length >= 3) {
      const x = Number(parts[1]);
      const y = Number(parts[2]);
      if (!Number.isNaN(x) && !Number.isNaN(y)) {
        cities.push({ x, y });
      }
    }
  }

  const reindexedCities = cities.map((city, idx) => ({ id: idx, ...city }));

  return {
    name: headers.NAME || "",
    comment: headers.COMMENT || "",
    type: headers.TYPE || "TSP",
    dimension: headers.DIMENSION ? Number(headers.DIMENSION) : reindexedCities.length,
    edgeWeightType: headers.EDGE_WEIGHT_TYPE || EDGE_WEIGHT_TYPE,
    cities: reindexedCities,
  };
};

export const parseVrpContent = (content) => {
  const lines = content.split(/\r?\n/);
  const headers = {};
  const coords = new Map();
  const demands = new Map();
  const depotIds = [];
  let section = null;

  for (const rawLine of lines) {
    const line = rawLine.trim();
    if (!line) continue;
    if (/^EOF$/i.test(line)) break;

    if (/^NODE_COORD_SECTION$/i.test(line)) {
      section = "NODE";
      continue;
    }
    if (/^DEMAND_SECTION$/i.test(line)) {
      section = "DEMAND";
      continue;
    }
    if (/^DEPOT_SECTION$/i.test(line)) {
      section = "DEPOT";
      continue;
    }

    if (!section) {
      const parsed = parseHeaderLine(line);
      if (parsed) headers[parsed.key] = parsed.value;
      continue;
    }

    if (section === "NODE") {
      const parts = line.split(/\s+/);
      if (parts.length >= 3) {
        const id = Number(parts[0]);
        const x = Number(parts[1]);
        const y = Number(parts[2]);
        if (!Number.isNaN(id) && !Number.isNaN(x) && !Number.isNaN(y)) {
          coords.set(id, { x, y });
        }
      }
    }

    if (section === "DEMAND") {
      const parts = line.split(/\s+/);
      if (parts.length >= 2) {
        const id = Number(parts[0]);
        const demand = Number(parts[1]);
        if (!Number.isNaN(id) && !Number.isNaN(demand)) {
          demands.set(id, demand);
        }
      }
    }

    if (section === "DEPOT") {
      const id = Number(line);
      if (Number.isNaN(id)) continue;
      if (id < 0) {
        section = null;
        continue;
      }
      if (id > 0) depotIds.push(id);
    }
  }

  const resolvedDepotIds = depotIds.length > 0 ? depotIds : [1];
  const depots = resolvedDepotIds.map((id, idx) => {
    const depotCoords = coords.get(id) ?? { x: 0, y: 0 };
    return { id: idx, nodeId: id, x: depotCoords.x, y: depotCoords.y };
  });
  const primaryDepot = depots[0] ?? { x: 0, y: 0 };

  const customers = Array.from(coords.entries())
    .filter(([id]) => !resolvedDepotIds.includes(id))
    .map(([id, point], idx) => ({
      id: idx,
      x: point.x,
      y: point.y,
      demand: demands.get(id) ?? 0,
      originalId: id,
    }));

  const name = headers.NAME || "";
  const vehicleMatch = name.match(/-k(\d+)/i);
  const numberOfVehicles = vehicleMatch ? Number(vehicleMatch[1]) : "";

  return {
    name,
    comment: headers.COMMENT || "",
    type: headers.TYPE || "CVRP",
    dimension: headers.DIMENSION ? Number(headers.DIMENSION) : customers.length + depots.length,
    edgeWeightType: headers.EDGE_WEIGHT_TYPE || EDGE_WEIGHT_TYPE,
    capacity: headers.CAPACITY ? Number(headers.CAPACITY) : "",
    numberOfVehicles,
    depot: { x: primaryDepot.x, y: primaryDepot.y },
    depots,
    customers,
  };
};
