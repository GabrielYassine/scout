import { useEffect, useState } from "react";

/**
 * Drives the animation progress used by charts.
 */
export function usePlayback({ length, initialSpeed = 50 }) {
  const [playbackSpeed, setPlaybackSpeed] = useState(initialSpeed);
  const [visibleCount, setVisibleCount] = useState(1);

  useEffect(() => {
    if (!length) return;

    const stepSize = Math.max(1, Math.floor(playbackSpeed / 15));

    const interval = setInterval(() => {
      setVisibleCount((prev) => {
        if (prev >= length) return prev;
        return Math.min(prev + stepSize, length);
      });
    }, 30);

    return () => clearInterval(interval);
  }, [length, playbackSpeed]);

  const resetPlayback = () => setVisibleCount(1);

  return {
    playbackSpeed,
    setPlaybackSpeed,
    visibleCount,
    resetPlayback,
  };
}
