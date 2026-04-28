import { useMemo, useState } from "react";

import {
  buildAverageRuns,
  normalizeSelectedRunKey,
} from "@/features/run/utils/runData.js";

export function useRunSelection({ batch, restoredRun, setSavedRun }) {
  const batches = useMemo(
    () => [...(batch?.batches ?? [])].sort((a, b) => a.runIndex - b.runIndex),
    [batch]
  );

  const averageByProblem = batch?.summary?.averageByProblem ?? {};
  const bestFitnessBoxPlotsByProblem =
    batch?.summary?.bestFitnessBoxPlotsByProblem ?? {};
  const averageRunTimeByProblem = batch?.summary?.averageRunTimeByProblem ?? {};

  const averageRuns = useMemo(
    () =>
      buildAverageRuns(
        averageByProblem,
        averageRunTimeByProblem,
        batch?.searchSpaceId
      ),
    [averageByProblem, averageRunTimeByProblem, batch?.searchSpaceId]
  );

  const [selectedRunKey, setSelectedRunKey] = useState(() => {
    if (restoredRun?.selectedRunKey != null) {
      return restoredRun.selectedRunKey;
    }
    return null;
  });

  const effectiveSelectedRunKey = normalizeSelectedRunKey(
    selectedRunKey,
    averageRuns,
    batches
  );

  function handleSelectedRunChange(value) {
    setSelectedRunKey(value);
    setSavedRun((prev) =>
      prev
        ? {
            ...prev,
            selectedRunKey: value,
          }
        : prev
    );
  }

  const selectedBatch =
    effectiveSelectedRunKey === "average"
      ? null
      : batches.find(
          (batchItem) =>
            String(batchItem.runIndex) === String(effectiveSelectedRunKey)
        ) ?? null;

  const runs =
    effectiveSelectedRunKey === "average"
      ? averageRuns
      : selectedBatch?.runs ?? [];

  return {
    batches,
    averageRuns,
    bestFitnessBoxPlotsByProblem,
    selectedBatch,
    runs,
    effectiveSelectedRunKey,
    handleSelectedRunChange,
  };
}