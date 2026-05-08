/**
 * Helpers for lab leftbar data parsing and catalog lookups.
 * @author s235257 & s230632
 */

const CATALOG_KEYS_BY_TYPE = {
  searchSpace: "searchSpaces",
  problem: "problems",
  generator: "generators",
  selection: "selectionRules",
  populationModel: "populationModels",
  parentSelectionRule: "parentSelectionRules",
  crossover: "crossovers",
  stopCondition: "stopConditions",
  observer: "observers",
};

export function parseValue(type, raw) {
  if (raw == null) return raw;

  if (type === "boolean") {
    return Boolean(raw);
  }

  // Numeric fields are kept empty while the user is editing an empty input.
  if (type === "int" || type === "long" || type === "double") {
    if (raw === "") return "";
    return Number(raw);
  }

  return raw;
}

export function findPieceDef(catalog, type, id) {
  if (!catalog || !id) return null;

  const catalogKey = CATALOG_KEYS_BY_TYPE[type];
  const list = catalogKey ? catalog[catalogKey] ?? [] : [];

  return list.find((item) => item.id === id) ?? null;
}

export function countPlacedPieces(puzzleConfig) {
  return Object.values(puzzleConfig ?? {}).reduce((sum, group) => {
    return sum + (Array.isArray(group) ? group.length : 0);
  }, 0);
}