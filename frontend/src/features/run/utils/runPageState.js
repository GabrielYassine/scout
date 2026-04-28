export function resolveRunPageState(locationState, savedRun) {
  const incomingRunId =
    locationState.runId ?? locationState.runRequest?.runId ?? null;
  const incomingStudyId =
    locationState.studyId ?? locationState.runtimeStudyRequest?.studyId ?? null;

  const savedMatchesIncomingRun =
    savedRun?.pageMode === "run" &&
    savedRun?.loading === false &&
    !!incomingRunId &&
    savedRun?.runId === incomingRunId;

  const savedMatchesIncomingStudy =
    savedRun?.pageMode === "runtimeStudy" &&
    savedRun?.loading === false &&
    !!incomingStudyId &&
    savedRun?.studyId === incomingStudyId;

  const shouldIgnoreIncomingState =
    locationState.loading === true &&
    (savedMatchesIncomingRun || savedMatchesIncomingStudy);

  const hasIncomingExecution =
    !shouldIgnoreIncomingState &&
    (Boolean(locationState.runId) ||
      Boolean(locationState.studyId) ||
      locationState.loading === true);

  const restoredRun = hasIncomingExecution ? null : savedRun;

  const pageMode =
    locationState.pageMode ??
    (locationState.runId ? "run" : null) ??
    (locationState.studyId ? "runtimeStudy" : null) ??
    restoredRun?.pageMode ??
    "run";

  const runRequest = locationState.runRequest ?? restoredRun?.runRequest ?? null;
  const runId =
    locationState.runId ?? runRequest?.runId ?? restoredRun?.runId ?? null;
  const studyId = locationState.studyId ?? restoredRun?.studyId ?? null;

  return {
    pageMode,
    runId,
    studyId,
    runRequest,
    runtimeStudyRequest:
      locationState.runtimeStudyRequest ??
      restoredRun?.runtimeStudyRequest ??
      null,
    puzzleConfig: locationState.puzzleConfig ?? restoredRun?.puzzleConfig ?? [],
    params: locationState.params ?? restoredRun?.params ?? [],
    tspInstance: locationState.tspInstance ?? restoredRun?.tspInstance ?? null,
    vrpInstance: locationState.vrpInstance ?? restoredRun?.vrpInstance ?? null,
    batchResponse: locationState.batch ?? restoredRun?.batch ?? null,
    studyPoints: restoredRun?.studyPoints ?? [],
    loading: shouldIgnoreIncomingState
      ? restoredRun?.loading ?? false
      : locationState.loading ?? restoredRun?.loading ?? false,
    error: locationState.error ?? restoredRun?.error ?? null,
    restoredRun,
    liveExecution: hasIncomingExecution,
  };
}