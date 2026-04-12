import { useEffect, useRef, useState } from "react";

/**
 * Small helper for request-style resources.
 * Contract:
 * - `loader` returns a promise.
 * - Handles cancellation on unmount.
 */
export function useAsyncResource(loader, deps = []) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const loaderRef = useRef(loader);
  loaderRef.current = loader;

  useEffect(() => {
    let cancelled = false;

    async function run() {
      setLoading(true);
      setError(null);
      try {
        const result = await loaderRef.current();
        if (!cancelled) setData(result);
      } catch (e) {
        if (!cancelled) setError(e?.message ?? String(e));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    run();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return { data, loading, error, setData };
}
