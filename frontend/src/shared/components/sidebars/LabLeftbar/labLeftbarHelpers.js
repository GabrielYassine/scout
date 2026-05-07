/**
 * Helpers for lab leftbar data parsing and lookups.
 * @author s235257 & s230632
 */

export function parseValue(type, raw) {
  if (raw == null) return raw;
  if (type === "boolean") return Boolean(raw);

  if (type === "int" || type === "long" || type === "double") {
    if (raw === "") return "";
    return Number(raw);
  }

  return raw;
}

export function findPieceDef(catalog, type, id) {
  if (!catalog || !id) return null;

  const catalogMap = {
    searchSpace: catalog.searchSpaces,
    problem: catalog.problems,
    generator: catalog.generators,
    selection: catalog.selectionRules,
    populationModel: catalog.populationModels,
    parentSelectionRule: catalog.parentSelectionRules,
    crossover: catalog.crossovers,
    stopCondition: catalog.stopConditions,
    observer: catalog.observers,
  };

  const list = catalogMap[type] ?? [];
  return list.find((item) => item.id === id) ?? null;
}

export function countPlacedPieces(puzzleConfig) {
  return Object.values(puzzleConfig ?? {}).reduce((sum, group) => {
    return sum + (Array.isArray(group) ? group.length : 0);
  }, 0);
}
