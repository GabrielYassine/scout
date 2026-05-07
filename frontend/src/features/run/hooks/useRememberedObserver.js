/**
  * Custom hook to manage the selected observer for a run chart, with persistence in localStorage.
  * @author s230632
 */
import { useEffect, useMemo, useState } from "react";

const STORAGE_PREFIX = "scout.runChart.selectedObserver";
// These functions handle localStorage access with error handling to avoid issues in unsupported environments or private browsing modes.
function readStoredObserver(storageKey) {
  try {
    return window.localStorage.getItem(storageKey);
  } catch {
    return null;
  }
}
// Writes the selected observer to localStorage, or removes it if no observer is selected.
function writeStoredObserver(storageKey, observerKey) {
  try {
    if (observerKey) {
      window.localStorage.setItem(storageKey, observerKey);
    } else {
      window.localStorage.removeItem(storageKey);
    }
  } catch {
    // Ignore localStorage errors.
  }
}
// The main hook manages the selected observer state, initializes it from localStorage, and updates localStorage when it changes.
export function useRememberedObserver({ problemId, displayKeys, initialObserver }) {
  const storageKey = useMemo(
    () => `${STORAGE_PREFIX}:${problemId ?? "unknown"}`,
    [problemId]
  );

  const [selectedObserver, setSelectedObserver] = useState(() => {
    const storedObserver = readStoredObserver(storageKey);

    return storedObserver && displayKeys.includes(storedObserver) ? storedObserver : initialObserver;
  });
// This effect runs when displayKeys, initialObserver, or storageKey change, and ensures the selected observer is valid and initialized from localStorage if possible.
  useEffect(() => {
    const storedObserver = readStoredObserver(storageKey);

    setSelectedObserver((current) => {
      if (displayKeys.includes(current)) {
        return current;
      }
      if (storedObserver && displayKeys.includes(storedObserver)) {
        return storedObserver;
      }

      return initialObserver;
    });
  }, [displayKeys, initialObserver, storageKey]);
// This effect updates localStorage whenever the selected observer changes, but only if it's a valid option in displayKeys.
  useEffect(() => {
    if (selectedObserver && displayKeys.includes(selectedObserver)) {
      writeStoredObserver(storageKey, selectedObserver);
    }
  }, [selectedObserver, displayKeys, storageKey]);

  return [selectedObserver, setSelectedObserver];
}