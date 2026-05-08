/**
 * Run execution context provider and state management.
 * This context manages the state of a run execution, including metadata, progress, and results.
 It also handles starting new runs, restoring runs from session storage, and streaming updates via websockets.
 * @author s235257
 */
import { createContext, useEffect, useMemo, useState } from "react";

import { useSessionStorageState } from "@/shared/hooks/useSessionStorageState.js";
import { normalizeBatch } from "@/features/run/utils/runData.js";
import { useRunWebSocket } from "@/features/run/hooks/useRunWebSocket.js";
import { useRuntimeStudyWebSocket } from "@/features/run/hooks/useRuntimeStudyWebSocket.js";

export const RunExecutionContext = createContext(null);

const EXECUTION_STORAGE_KEY = "scout:currentExecution";
const SESSION_ID_STORAGE_KEY = "scout:sessionId";

function getInitialStudyStatus(savedExecution) {
  if (savedExecution?.pageMode !== "runtimeStudy") return null;
  if (savedExecution?.error) return "FAILED";
  if (savedExecution?.loading) return "ONGOING";
  return "FINISHED";
}

export function RunExecutionProvider({ children }) {
  const [savedRun, setSavedRun] = useSessionStorageState(
    EXECUTION_STORAGE_KEY,
    null
  );

  // Remove the execution session on tab close. This ensures that if the user starts a run.
  useEffect(() => {
    const clearExecutionSession = () => {
      sessionStorage.removeItem(SESSION_ID_STORAGE_KEY);
    };

    window.addEventListener("pagehide", clearExecutionSession);

    return () => {
      window.removeEventListener("pagehide", clearExecutionSession);
    };
  }, []);

  // Execution metadata. This is initialized from sessionStorage so navigation
  // inside the app keeps the current tab's execution result, but refresh and
  // tab close forget it.
  const [pageMode, setPageMode] = useState(savedRun?.pageMode ?? "run");
  const [runId, setRunId] = useState(savedRun?.runId ?? null);
  const [studyId, setStudyId] = useState(savedRun?.studyId ?? null);
  const [runRequest, setRunRequest] = useState(savedRun?.runRequest ?? null);
  const [runtimeStudyRequest, setRuntimeStudyRequest] = useState(
    savedRun?.runtimeStudyRequest ?? null
  );

  const [puzzleConfig, setPuzzleConfig] = useState(
    savedRun?.puzzleConfig ?? []
  );
  const [params, setParams] = useState(savedRun?.params ?? []);
  const [tspInstance, setTspInstance] = useState(savedRun?.tspInstance ?? null);
  const [vrpInstance, setVrpInstance] = useState(savedRun?.vrpInstance ?? null);

  // Result state. Websocket hooks update this while a run is ongoing.
  const [batch, setBatch] = useState(() =>
    normalizeBatch(savedRun?.batch ?? null)
  );
  const [studyPoints, setStudyPoints] = useState(
    () => savedRun?.studyPoints ?? []
  );
  const [loading, setLoading] = useState(() => !!savedRun?.loading);
  const [error, setError] = useState(savedRun?.error ?? null);
  const [studyStatus, setStudyStatus] = useState(() =>
    getInitialStudyStatus(savedRun)
  );

  // loading only means "waiting for first visible result".
  // streaming means "keep the websocket alive".
  const [streaming, setStreaming] = useState(false);

  const liveExecution =
    streaming &&
    ((pageMode === "run" && !!runId && !!runRequest) ||
      (pageMode === "runtimeStudy" && !!studyId && !!runtimeStudyRequest));

  const restoredRun = liveExecution ? null : savedRun;

  function startRunExecution({
    runId,
    runRequest,
    puzzleConfig,
    params,
    tspInstance,
    vrpInstance,
  }) {
    const nextSavedRun = {
      pageMode: "run",
      loading: true,
      runId,
      studyId: null,
      batch: null,
      studyPoints: [],
      puzzleConfig,
      params,
      tspInstance,
      vrpInstance,
      runRequest,
      runtimeStudyRequest: null,
      selectedRunKey: "0",
      savedAt: Date.now(),
    };

    setSavedRun(nextSavedRun);

    setPageMode("run");
    setRunId(runId);
    setStudyId(null);
    setRunRequest(runRequest);
    setRuntimeStudyRequest(null);

    setPuzzleConfig(puzzleConfig);
    setParams(params);
    setTspInstance(tspInstance);
    setVrpInstance(vrpInstance);

    setBatch(null);
    setStudyPoints([]);
    setLoading(true);
    setError(null);
    setStudyStatus(null);
    setStreaming(true);
  }

  function startRuntimeStudyExecution({
    studyId,
    runtimeStudyRequest,
    puzzleConfig,
    params,
    tspInstance,
    vrpInstance,
  }) {
    const nextSavedRun = {
      pageMode: "runtimeStudy",
      loading: true,
      runId: null,
      studyId,
      batch: null,
      studyPoints: [],
      puzzleConfig,
      params,
      tspInstance,
      vrpInstance,
      runRequest: null,
      runtimeStudyRequest,
      savedAt: Date.now(),
    };

    setSavedRun(nextSavedRun);

    setPageMode("runtimeStudy");
    setRunId(null);
    setStudyId(studyId);
    setRunRequest(null);
    setRuntimeStudyRequest(runtimeStudyRequest);

    setPuzzleConfig(puzzleConfig);
    setParams(params);
    setTspInstance(tspInstance);
    setVrpInstance(vrpInstance);

    setBatch(null);
    setStudyPoints([]);
    setLoading(true);
    setError(null);
    setStudyStatus("ONGOING");
    setStreaming(true);
  }

  function clearExecution() {
    setSavedRun(null);

    setPageMode("run");
    setRunId(null);
    setStudyId(null);
    setRunRequest(null);
    setRuntimeStudyRequest(null);

    setPuzzleConfig([]);
    setParams([]);
    setTspInstance(null);
    setVrpInstance(null);

    setBatch(null);
    setStudyPoints([]);
    setLoading(false);
    setError(null);
    setStudyStatus(null);
    setStreaming(false);
  }

  // Normal runs stream incremental batch/run updates.
  useRunWebSocket({
    enabled: liveExecution && pageMode === "run",
    runId,
    runRequest,
    puzzleConfig,
    params,
    tspInstance,
    vrpInstance,
    setLoading,
    setError,
    setBatch,
    setSavedRun,
    setStreaming,
  });

  // Runtime studies stream one aggregated study point per problem size.
  useRuntimeStudyWebSocket({
    enabled: liveExecution && pageMode === "runtimeStudy",
    studyId,
    runtimeStudyRequest,
    puzzleConfig,
    params,
    tspInstance,
    vrpInstance,
    setLoading,
    setError,
    setStudyPoints,
    setSavedRun,
    setStudyStatus,
    setStreaming,
  });

  const value = useMemo(
    () => ({
      pageMode,
      runId,
      studyId,
      runRequest,
      runtimeStudyRequest,
      puzzleConfig,
      params,
      tspInstance,
      vrpInstance,
      restoredRun,
      liveExecution,
      streaming,

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

      startRunExecution,
      startRuntimeStudyExecution,
      clearExecution,
    }),
    [
      pageMode,
      runId,
      studyId,
      runRequest,
      runtimeStudyRequest,
      puzzleConfig,
      params,
      tspInstance,
      vrpInstance,
      restoredRun,
      liveExecution,
      streaming,
      savedRun,
      setSavedRun,
      batch,
      studyPoints,
      loading,
      error,
      studyStatus,
    ]
  );

  return (
    <RunExecutionContext.Provider value={value}>
      {children}
    </RunExecutionContext.Provider>
  );
}