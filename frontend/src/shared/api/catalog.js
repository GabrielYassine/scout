import { fetchJson } from "./http.js";

export function getCatalog() {
  return fetchJson("/api/catalog");
}
