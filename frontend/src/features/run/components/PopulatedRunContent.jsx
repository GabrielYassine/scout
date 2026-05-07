/**
  *
  * @author s235257 & s230632
 */
import RunChart from "@/features/run/components/charts/run/RunChart.jsx";
import RuntimeStudyChart from "@/features/run/components/charts/study/RuntimeStudyChart.jsx";

export default function PopulatedRunContent({
  pageMode,
  runtimeStudyProblemId,
  studyPoints,
  studyStatus,
  runs,
  selectedBatch,
  visibleCount,
  effectiveSelectedRunKey,
  layoutMode,
  tspInstance,
  vrpInstance,
  bestFitnessBoxPlotsByProblem,
}) {
  if (pageMode === "runtimeStudy") {
    return (
      <RuntimeStudyChart
        studyTitle="Runtime Study"
        problemId={runtimeStudyProblemId}
        points={studyPoints}
        visibleCount={visibleCount}
        studyStatus={studyStatus}
      />
    );
  }

  return (
    <div
      className={`run-stack ${
        layoutMode === "grid" ? "run-stack--grid" : "run-stack--stack"
      }`}
    >
      {runs.map((run, idx) => (
        <RunChart
          key={`${effectiveSelectedRunKey}-${idx}`}
          run={run}
          runIndex={selectedBatch?.runIndex ?? "average"}
          visibleCount={visibleCount}
          instanceName={
            run.problemId === "tsp"
              ? tspInstance?.name ?? null
              : run.problemId === "vrp"
              ? vrpInstance?.name ?? null
              : null
          }
          // Average runs can include backend-generated boxplots.
          // Individual runs show their direct logged series instead.
          bestFitnessBoxPlot={effectiveSelectedRunKey === "average" ? bestFitnessBoxPlotsByProblem[run.problemId] ?? null : null
          }
        />
      ))}
    </div>
  );
}