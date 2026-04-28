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

// Sorts study points by problem size.
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
  const readySentRef = useRef(false);
  const startSentRef = useRef(false);
  const latestPointsRef = useRef([]);

  useEffect(() => {
    if (!enabled || !studyId || !runtimeStudyRequest) return;

    readySentRef.current = false;
    startSentRef.current = false;
    latestPointsRef.current = [];

    const client = new Client({
      brokerURL: createWebSocketUrl(),
      reconnectDelay: 0,
    });

    const handleStudyConnected = () => {
      if (startSentRef.current) return;

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

      const nextPoints = sortStudyPoints([
        ...(latestPointsRef.current ?? []).filter(
          (p) => Number(p.problemSize) !== Number(message.point.problemSize)
        ),
        message.point,
      ]);

      latestPointsRef.current = nextPoints;
      setStudyPoints(nextPoints);

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