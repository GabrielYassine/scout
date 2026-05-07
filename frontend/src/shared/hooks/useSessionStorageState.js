/**
 * React hook for sessionStorage-backed state.
 * This is like useState, but the value is persisted in sessionStorage and shared across tabs.
 * @author s235257
 */

import { useEffect, useState } from "react";

export function useSessionStorageState(key, initialValue) {
  const [value, setValue] = useState(() => {
    try {
      const raw = sessionStorage.getItem(key);
      return raw != null ? JSON.parse(raw) : initialValue;
    } catch {
      return initialValue;
    }
  });

  useEffect(() => {
    try {
      sessionStorage.setItem(key, JSON.stringify(value));
    } catch {
      // ignore storage errors
    }
  }, [key, value]);

  const reset = () => setValue(initialValue);

  return [value, setValue, reset];
}