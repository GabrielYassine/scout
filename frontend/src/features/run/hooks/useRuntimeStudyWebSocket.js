/**
 * Manages the websocket lifecycle for runtime studies.
 * Study points are merged by problem size so repeated updates replace older points.
 */
import { useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";

import { createWebSocketUrl, parseSocketMessage } from "@/features/run/utils/socketUtils.js";

// Runtime study points are shown by problem size, so keeping them sorted
// makes the chart stable even when websocket messages arrive in another order.
function sortStudyPoints(points) {
  return [...points].sort(
    (a, b) => Number(a.problemSize) - Number(b.problemSize)
  );
}

export function useRuntimeStudyWebSocket({
  enabled,
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
}) {
  // These refs track websocket lifecycle state without causing re-renders.
  const startSentRef = useRef(false);
  const latestPointsRef = useRef([]);

  useEffect(() => {
    if (!enabled || !studyId || !runtimeStudyRequest) return;

    // Reset per-study lifecycle state when connecting to a new live study.
    startSentRef.current = false;
    latestPointsRef.current = [];

    const client = new Client({
      brokerURL: createWebSocketUrl(),
      reconnectDelay: 0,
    });

    client.onWebSocketClose = (event) => {
      console.log("[Study WS] disconnected", {
        studyId,
        code: event.code,
        reason: event.reason,
      });
    };

    const handleStudyProgress = (message) => {
      if (!message.point) return;

      setLoading(false);
      setStudyStatus("ONGOING");

      // Runtime studies report one aggregated point per problem size.
      // If a repeated update arrives for the same size, replace the old point.
      const nextPoints = sortStudyPoints([
        ...latestPointsRef.current.filter(
          (point) =>
            Number(point.problemSize) !== Number(message.point.problemSize)
        ),
        message.point,
      ]);

      latestPointsRef.current = nextPoints;
      setStudyPoints(nextPoints);
    };

    const handleStudyFinished = () => {
      setLoading(false);
      setStreaming(false);
      setStudyStatus("FINISHED");

      // Persist only the finished study. Saving partial progress as finished would
      // make RunPage restore it and disconnect the websocket too early.
      setSavedRun({
        pageMode: "runtimeStudy",
        studyId,
        batch: null,
        studyPoints: latestPointsRef.current,
        loading: false,
        puzzleConfig,
        params,
        tspInstance,
        vrpInstance,
        runtimeStudyRequest,
        savedAt: Date.now(),
      });

      client.deactivate();
    };

    const handleStudyFailed = (message) => {
      setLoading(false);
      setStreaming(false);
      setStudyStatus("FAILED");
      setError(message.message || "Runtime study failed");
      client.deactivate();
    };

    const handleSocketMessage = (rawMessage) => {
      const message = parseSocketMessage(rawMessage);
      if (!message.studyId || message.studyId !== studyId) return;

      switch (message.type) {
        case "STUDY_PROGRESS":
          handleStudyProgress(message);
          return;

        case "STUDY_FINISHED":
          handleStudyFinished();
          return;

        case "STUDY_FAILED":
          handleStudyFailed(message);
          return;

        default:
          return;
      }
    };

    client.onConnect = () => {
      client.subscribe(`/topic/study/${studyId}`, handleSocketMessage);

      if (startSentRef.current) return;

      // Start the study after subscribing so progress packets are not missed.
      startSentRef.current = true;
      setStudyStatus("ONGOING");

      client.publish({
        destination: `/app/study/${studyId}/start`,
        body: JSON.stringify({
          sessionId: runtimeStudyRequest.sessionId,
        }),
      });
    };

    client.activate();

    return () => {
      try {
        client.deactivate();
      } catch {
        // ignore cleanup errors
      }
    };
  }, [
    enabled,
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
  ]);
}