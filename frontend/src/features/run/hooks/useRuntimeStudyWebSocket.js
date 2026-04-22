import { useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";

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
  const readySentRef = useRef(false);
  const startSentRef = useRef(false);

  useEffect(() => {
    if (!enabled) return;
    if (!studyId || !runtimeStudyRequest) return;

    readySentRef.current = false;
    startSentRef.current = false;

    const wsUrl = `${window.location.protocol === "https:" ? "wss" : "ws"}://${window.location.host}/ws`;

    const client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 0,
    });

    client.onConnect = () => {
      client.subscribe(`/topic/study/${studyId}`, (message) => {
        let data;
        try {
          data = JSON.parse(message.body);
        } catch {
          return;
        }

        if (!data?.studyId || data.studyId !== studyId) return;

        if (data.type === "STUDY_CONNECTED") {
          if (!startSentRef.current) {
            startSentRef.current = true;
            client.publish({
              destination: `/app/study/${studyId}/start`,
              body: JSON.stringify(runtimeStudyRequest),
            });
          }
          return;
        }

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
            runtimeStudyRequest,
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
  ]);
}