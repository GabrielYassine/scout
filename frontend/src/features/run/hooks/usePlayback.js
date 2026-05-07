/**
  * Custom hook to manage playback of a run's chart. It controls the speed of the animation and how many points are currently visible in the chart.
  * @author s230632
 */

import { useEffect,  useRef,  useState } from "react";

const PLAYBACK_TICK_MS = 30;
const PLAYBACK_DIVISOR = 15;
export function usePlayback({ length, initialSpeed = 1,  showAllImmediately = false, }) {
  const [playbackSpeed, setPlaybackSpeed] = useState(initialSpeed);
  // visibleCount controls how many points are currently shown in the chart.
  const [visibleCount, setVisibleCount] = useState(1);
  const hasAppliedInitialShowAllRef = useRef(false);
  // If the user reopens a previously existing run, show the full graph once
  // instead of replaying the animation from the beginning.
  useEffect(() => {
    if (
      showAllImmediately &&
      length > 0 &&
      !hasAppliedInitialShowAllRef.current
    ) {
      setVisibleCount(length);
      hasAppliedInitialShowAllRef.current = true;
    }
  }, [length, showAllImmediately]);

  useEffect(() => {
    if (!length) return;

    if (showAllImmediately && !hasAppliedInitialShowAllRef.current) {
      return;
    }

    if (visibleCount >= length) return;

    // Convert the slider value into the number of points revealed per tick.
    const stepSize = Math.max(1, Math.floor(playbackSpeed / PLAYBACK_DIVISOR));

    const intervalId = setInterval(() => {
      setVisibleCount((previousCount) =>
        previousCount >= length
          ? previousCount
          : Math.min(previousCount + stepSize, length)
      );
    }, PLAYBACK_TICK_MS);

    return () => clearInterval(intervalId);
  }, [length, playbackSpeed, visibleCount, showAllImmediately]);

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