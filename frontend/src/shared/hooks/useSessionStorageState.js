/*
 * useSessionStorageState stores a React state value in sessionStorage
 * so it persists across page reloads within the same browser session.
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