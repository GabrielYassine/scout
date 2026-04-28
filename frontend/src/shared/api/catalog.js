/*
 * API helper for fetching the lab catalog.
 */
import { fetchJson } from "./http.js";

export function getCatalog() {
  return fetchJson("/api/catalog");
}
