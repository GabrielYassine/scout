import { fetchJson } from "./http.js";

export function getTemplates() {
  return fetchJson("/api/templates");
}
