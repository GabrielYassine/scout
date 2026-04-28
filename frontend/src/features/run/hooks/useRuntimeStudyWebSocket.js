/**
 * Manages the websocket lifecycle for runtime studies.
 * Study points are merged by problem size so repeated updates replace older points.
 */
import { useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";

import {
  createWebSocketUrl,
  parseSocketMessage,
} from "@/features/run/utils/socketUtils.js";

// Runtime study points are shown by problem size, so keeping them sorted
// makes the chart stable even when websocket messages arrive in another order.
function sortStudyPoints(points) {
  return [...(points ?? [])].sort(
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
}) {
  // These refs track websocket lifecycle state without causing re-renders.
  const readySentRef = useRef(false);
  const startSentRef = useRef(false);
  const latestPointsRef = useRef([]);

  useEffect(() => {
    if (!enabled || !studyId || !runtimeStudyRequest) return;

    // Reset per-study lifecycle state when connecting to a new live study.
    readySentRef.current = false;
    startSentRef.current = false;
    latestPointsRef.current = [];

    const client = new Client({
      brokerURL: createWebSocketUrl(),
      reconnectDelay: 0,
    });

    const handleStudyConnected = () => {
      if (startSentRef.current) return;

      // The frontend first subscribes and sends "ready". The backend then
      // confirms STUDY_CONNECTED, after which the actual study is started.
      startSentRef.current = true;
      setStudyStatus("ONGOING");

      client.publish({
        destination: `/app/study/${studyId}/start`,
        body: JSON.stringify(runtimeStudyRequest),
      });
    };

    const handleStudyFinished = () => {
      setLoading(false);
      setStudyStatus("FINISHED");

      // Persist the finished study so the RunPage can be restored after refresh
      // or navigation without reconnecting to the websocket.
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

    const handleStudyProgress = (message) => {
      if (!message.point) return;

      setLoading(false);
      setStudyStatus("ONGOING");

      // Each problem size should appear once in the chart. If a newer point
      // arrives for the same size, it replaces the older one.
      const nextPoints = sortStudyPoints([
        ...(latestPointsRef.current ?? []).filter(
          (p) => Number(p.problemSize) !== Number(message.point.problemSize)
        ),
        message.point,
      ]);

      latestPointsRef.current = nextPoints;
      setStudyPoints(nextPoints);

      // Save partial progress as well, so refreshing during a running study
      // still restores the newest received points.
      setSavedRun((prev) => ({
        ...(prev ?? {}),
        pageMode: "runtimeStudy",
        studyId,
        batch: null,
        studyPoints: nextPoints,
        loading: false,
        puzzleConfig,
        params,
        tspInstance,
        vrpInstance,
        runtimeStudyRequest,
        savedAt: Date.now(),
      }));
    };

    const handleStudyFailed = (message) => {
      setLoading(false);
      setStudyStatus("FAILED");
      setError(message.message || "Runtime study failed");
      client.deactivate();
    };

    const handleSocketMessage = (rawMessage) => {
      const message = parseSocketMessage(rawMessage);
      if (!message?.studyId || message.studyId !== studyId) return;

      switch (message.type) {
        case "STUDY_CONNECTED":
          handleStudyConnected();
          return;

        case "STUDY_PROGRESS":
          handleStudyProgress(message);
          return;

        case "STUDY_FINISHED":
          handleStudyFinished(message);
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

      if (!readySentRef.current) {
        // Tell the backend that the frontend is subscribed and ready to receive
        // progress before the backend starts the study.
        readySentRef.current = true;
        client.publish({
          destination: `/app/study/${studyId}/ready`,
          body: JSON.stringify({ studyId }),
        });
      }
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
  ]);
}