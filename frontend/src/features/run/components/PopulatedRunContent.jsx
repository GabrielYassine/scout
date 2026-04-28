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
      className={`run-stack ${layoutMode === "grid" ? "run-stack--grid" : "run-stack--stack"}`}
    >
      {runs.map((run, idx) => (
        <RunChart
          key={`${effectiveSelectedRunKey}-${idx}`}
          run={run}
          runIndex={selectedBatch?.runIndex ?? "average"}
          visibleCount={visibleCount}
          instanceName={
            run.problemId === "tsp"? tspInstance?.name ?? null: run.problemId === "vrp"? vrpInstance?.name ?? null: null
          }
          bestFitnessBoxPlot={effectiveSelectedRunKey === "average"? bestFitnessBoxPlotsByProblem[run.problemId] ?? null: null
          }
        />
      ))}
    </div>
  );
}