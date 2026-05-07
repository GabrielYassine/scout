/**
  *
  * @author s230632
 */
import { useEffect, useMemo, useState } from "react";

const STORAGE_PREFIX = "scout.runChart.selectedObserver";

function readStoredObserver(storageKey) {
  try {
    return window.localStorage.getItem(storageKey);
  } catch {
    return null;
  }
}

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

export function useRememberedObserver({ problemId, displayKeys, initialObserver }) {
  const storageKey = useMemo(
    () => `${STORAGE_PREFIX}:${problemId ?? "unknown"}`,
    [problemId]
  );

  const [selectedObserver, setSelectedObserver] = useState(() => {
    const storedObserver = readStoredObserver(storageKey);

    return storedObserver && displayKeys.includes(storedObserver) ? storedObserver : initialObserver;
  });

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

  useEffect(() => {
    if (selectedObserver && displayKeys.includes(selectedObserver)) {
      writeStoredObserver(storageKey, selectedObserver);
    }
  }, [selectedObserver, displayKeys, storageKey]);

  return [selectedObserver, setSelectedObserver];
}