/**
  * Utility functions for managing the session id in sessionStorage.
  * This allows the lab feature to persist the session id across page reloads within the same browser tab.
  * @author s235257
 */

const SESSION_STORAGE_KEY = "scout:sessionId";

// Read the current session id from sessionStorage.
export function getExistingSessionId() {
  return window.sessionStorage?.getItem(SESSION_STORAGE_KEY) ?? null;
}

// Save the current session id to sessionStorage.
export function persistSessionId(sessionId) {
  try {
    window.sessionStorage?.setItem(SESSION_STORAGE_KEY, sessionId);
  } catch {
    // Ignore storage errors.
  }
}