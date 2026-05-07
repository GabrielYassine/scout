/**
  *
  * @author s235257
 */

import { fetchJson } from "./http.js";

export function getCatalog() {
  return fetchJson("/api/catalog");
}
