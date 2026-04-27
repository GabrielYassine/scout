/**
 * usePlayback is a custom hook for chart animation.
 * It controls playback speed, gradually reveals more data points,
 * and provides a reset function to start the playback over.
*/
import { useEffect, useState } from "react";

const PLAYBACK_TICK_MS = 30;
const PLAYBACK_DIVISOR = 15;

/**
 * Drives the animation progress used by charts.
 */
export function usePlayback({ length, initialSpeed = 50 }) {
  const [playbackSpeed, setPlaybackSpeed] = useState(initialSpeed);
  const [visibleCount, setVisibleCount] = useState(1);
  useEffect(() => {
    if (!length) return;
    // Calculate how many data points to reveal on each tick based on the current playback speed.
    const stepSize = Math.max(1, Math.floor(playbackSpeed / PLAYBACK_DIVISOR));
    // Set up an interval to incrementally increase the visible count until it reaches the total length.
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