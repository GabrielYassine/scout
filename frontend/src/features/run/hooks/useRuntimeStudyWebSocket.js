import { useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";

import { startRuntimeStudy } from "@/shared/api/run.js";

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
}) {
  const studyStartedRef = useRef(false);

  useEffect(() => {
    if (!enabled) return;
    if (!studyId || !runtimeStudyRequest) return;

    const wsUrl = `${window.location.protocol === "https:" ? "wss" : "ws"}://${window.location.host}/ws`;

    const client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 0,
    });

    client.onConnect = async () => {
      client.subscribe(`/topic/study/${studyId}`, (message) => {
        const data = JSON.parse(message.body);

        if (data.type === "STUDY_FINISHED") {
          const sortedPoints = [...(data.study?.points ?? [])].sort(
            (a, b) => Number(a.problemSize) - Number(b.problemSize)
          );
          setLoading(false);
          setStudyPoints(sortedPoints);
          setSavedRun({
            pageMode: "runtimeStudy",
            batch: null,
            studyPoints: sortedPoints,
            loading: false,
            puzzleConfig,
            params,
            tspInstance,
            vrpInstance,
            savedAt: Date.now(),
          });
          client.deactivate();
          return;
        }

        if (data.type === "STUDY_FAILED") {
          setLoading(false);
          setError(data.message || "Runtime study failed");
          client.deactivate();
        }
      });

      if (!studyStartedRef.current) {
        studyStartedRef.current = true;
        try {
          console.log("POST /api/runtime-study payload:", runtimeStudyRequest);
          await startRuntimeStudy(runtimeStudyRequest);
        } catch (err) {
          setLoading(false);
          setError(err.message || "Failed to start runtime study");
        }
      }
    };

    client.onStompError = (frame) => {
      console.error("Study WebSocket STOMP error", frame.headers["message"], frame.body);
    };

    client.onWebSocketError = (event) => {
      console.error("Study WebSocket transport error", event);
    };

    client.activate();

    return () => {
      try {
        client.deactivate();
      } catch (e) {
        console.error("Failed to close study WebSocket", e);
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
  ]);
}
