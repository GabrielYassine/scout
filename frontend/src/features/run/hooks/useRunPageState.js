/**
  *
  * @author s235257 & s230632
 */

import { useMemo, useState } from "react";
import { useLocation } from "react-router-dom";

import { useSessionStorageState } from "@/shared/hooks/useSessionStorageState.js";
import { normalizeBatch } from "@/features/run/utils/runData.js";
import { resolveRunPageState } from "@/features/run/utils/runPageState.js";

export function useRunPageState() {
  const location = useLocation();
  const [savedRun, setSavedRun] = useSessionStorageState("scout:lastRun", null);

  const locationState = location.state ?? {};

  // Combines router state and saved localStorage state into one initial page state.
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

  // These states are initialized from resolvedState once, then updated by
  // websocket hooks or user interaction while the page is mounted.
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