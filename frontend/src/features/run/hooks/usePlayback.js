/**
 * usePlayback is a custom hook for chart animation.
 * It controls playback speed, gradually reveals more data points,
 * and provides a reset function to start the playback over.
*/
import { useEffect, useState } from "react";

const PLAYBACK_TICK_MS = 30;
const PLAYBACK_DIVISOR = 15;

export function usePlayback({ length, initialSpeed = 50 }) {
  const [playbackSpeed, setPlaybackSpeed] = useState(initialSpeed);
  const [visibleCount, setVisibleCount] = useState(1);
  useEffect(() => {
    if (!length) return;
    const stepSize = Math.max(1, Math.floor(playbackSpeed / PLAYBACK_DIVISOR));
    const intervalId = setInterval(() => {
      setVisibleCount((previousCount) =>
        previousCount >= length? previousCount : Math.min(previousCount + stepSize, length)
      );
    }, PLAYBACK_TICK_MS);

    return () => clearInterval(intervalId);
  }, [length, playbackSpeed]);

  function resetPlayback() {
    setVisibleCount(1);
  }

  return {
    playbackSpeed,
    setPlaybackSpeed,
    visibleCount,
    resetPlayback,
  };
}