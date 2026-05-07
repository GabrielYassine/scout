/**
  * Custom hook to manage run selection state and logic for a batch of runs.
   * It derives available runs, the currently selected run, and handlers to change selection.
  * @author s230632
 */

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

  // Average runs are synthetic runs built from backend summary data.
  const averageRuns = useMemo(
    () =>
      buildAverageRuns(
        averageByProblem,
        averageRunTimeByProblem,
        batch?.searchSpaceId
      ).map((run) => ({
        ...run,
        status: "finished",
        isAverageRun: true,
      })),
    [averageByProblem, averageRunTimeByProblem, batch?.searchSpaceId]
  );

  const [selectedRunKey, setSelectedRunKey] = useState(() => {
    if (restoredRun?.selectedRunKey != null) {
      return restoredRun.selectedRunKey;
    }
    return null;
  });

  // Ensures the selected key still points to an available run after data changes.
  const effectiveSelectedRunKey = normalizeSelectedRunKey(
    selectedRunKey,
    averageRuns,
    batches
  );

  function handleSelectedRunChange(value) {
    setSelectedRunKey(value);

    // Persist the selection so refresh/navigation restores the same view.
    setSavedRun((prev) =>
      prev ? {...prev, selectedRunKey: value } : prev
    );
  }

  const selectedBatch =
    effectiveSelectedRunKey === "average" ? null : batches.find(
      (batchItem) => String(batchItem.runIndex) === String(effectiveSelectedRunKey)
    ) ?? null;

  const runs =
    effectiveSelectedRunKey === "average" ? averageRuns : selectedBatch?.runs ?? [];

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