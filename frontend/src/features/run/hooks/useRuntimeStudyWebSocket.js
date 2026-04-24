import { useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";

function createWebSocketUrl() {
  const protocol = window.location.protocol === "https:" ? "wss" : "ws";
  return `${protocol}://${window.location.host}/ws`;
}

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

         setStudyPoints((prev) => {
           const filtered = (prev ?? []).filter(
             (p) => Number(p.problemSize) !== Number(message.point.problemSize)
           );

           const next = sortStudyPoints([...filtered, message.point]);
           latestPointsRef.current = next;
           return next;
         });

         setSavedRun((prev) => {
           const nextPoints = sortStudyPoints([
             ...((prev?.studyPoints ?? []).filter(
               (p) => Number(p.problemSize) !== Number(message.point.problemSize)
             )),
             message.point,
           ]);

           latestPointsRef.current = nextPoints;

           return {
             ...(prev ?? {}),
             pageMode: "runtimeStudy",
             studyId,
             batch: null,
             studyPoints: nextPoints,
             loading: true,
             puzzleConfig,
             params,
             tspInstance,
             vrpInstance,
             runtimeStudyRequest,
             savedAt: Date.now(),
           };
         });
   };

    const handleStudyFailed = (message) => {
      setLoading(false);
      setStudyStatus("FAILED");
      setError(message.message || "Runtime study failed");
      client.deactivate();
    };

    const handleSocketMessage = (rawMessage) => {
      let message;
      try {
        message = JSON.parse(rawMessage.body);
      } catch {
        return;
      }

      if (!message?.studyId || message.studyId !== studyId) return;

      switch (message.type) {
        case "STUDY_CONNECTED":
          handleStudyConnected();
          return;

        case "STUDY_PROGRESS":
            console.log("Received study progress:", message);
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