/**
 * Resolves and owns the RunPage state restored from router state and localStorage.
 * Live executions take priority over saved results unless the saved result already
 * matches the incoming run/study id.
 */
import { useMemo, useState } from "react";
import { useLocation } from "react-router-dom";

import { useLocalStorageState } from "@/shared/hooks/useLocalStorageState.js";
import { normalizeBatch } from "@/features/run/utils/runData.js";
import { resolveRunPageState } from "@/features/run/utils/runPageState.js";

export function useRunPageState() {
  const location = useLocation();
  const [savedRun, setSavedRun] = useLocalStorageState("scout:lastRun", null);

  const locationState = location.state ?? {};

  const resolvedState = useMemo(
    () => resolveRunPageState(locationState, savedRun),
    [locationState, savedRun]
  );

  const {
    batchResponse,
    studyPoints: initialStudyPoints,
    loading: initialLoading,
    error: initialError,
    pageMode,
  } = resolvedState;

  const [batch, setBatch] = useState(() =>
    normalizeBatch(batchResponse ?? null)
  );
  const [studyPoints, setStudyPoints] = useState(() => initialStudyPoints);
  const [loading, setLoading] = useState(!!initialLoading);
  const [error, setError] = useState(initialError ?? null);

  const [studyStatus, setStudyStatus] = useState(() => {
    if (pageMode !== "runtimeStudy") return null;
    if (initialError) return "FAILED";
    if (initialLoading) return "ONGOING";
    return "FINISHED";
  });

  return {
    ...resolvedState,

    savedRun,
    setSavedRun,

    batch,
    setBatch,

    studyPoints,
    setStudyPoints,

    loading,
    setLoading,

    error,
    setError,

    studyStatus,
    setStudyStatus,
  };
}