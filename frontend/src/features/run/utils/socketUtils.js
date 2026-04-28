/**
 * Shared websocket helpers used by run and runtime-study hooks.
 */
export function createWebSocketUrl() {
  const protocol = window.location.protocol === "https:" ? "wss" : "ws";
  return `${protocol}://${window.location.host}/ws`;
}

export function parseSocketMessage(rawMessage) {
  try {
    return JSON.parse(rawMessage.body);
  } catch {
    return null;
  }
}