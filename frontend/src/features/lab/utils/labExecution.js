/**
  * Utility functions for preparing and validating data before starting a runtime study or a standard run in the lab.
  * These functions handle tasks such as parsing user input
  * checking for required problem instances, and building the request payloads for the backend API.
  * @author s235257 & s230632
 */

import { getExistingSessionId } from "@/features/lab/utils/sessionStorage.js";

// Parse a comma-separated string into an array of valid positive integers.
export function parseProblemSizes(text) {
  return String(text ?? "")
    .split(",")
    .map((s) => Number(s.trim()))
    .filter((n) => Number.isInteger(n) && n > 0);
}

// Check which problem types are included in the selected configuration.
export function getProblemFlags(problemList) {
  return {
    isTspProblem: problemList.some((p) => p.id === "tsp"),
    isVrpProblem: problemList.some((p) => p.id === "vrp"),
  };
}

// Ensure that all required problem instances exist before starting an execution.
export function validateProblemInstances({ problemList, tspInstance, vrpInstance }) {
  const { isTspProblem, isVrpProblem } = getProblemFlags(problemList);

  const hasValidTspInstance =
    Array.isArray(tspInstance?.cities) && tspInstance.cities.length > 0;

  const hasValidVrpInstance =
    Array.isArray(vrpInstance?.customers) &&
    vrpInstance.customers.length > 0 &&
    vrpInstance?.depot != null;

  if (isTspProblem && !hasValidTspInstance) {
    throw new Error("Please upload or create a TSP instance before running a TSP problem.");
  }

  if (isVrpProblem && !hasValidVrpInstance) {
    throw new Error("Please upload or create a VRP instance before running a VRP problem.");
  }

  return { isTspProblem, isVrpProblem };
}

// Build the problemParams object sent to the backend.
export function buildProblemParams({
  baseProblemParams,
  tspInstance,
  vrpInstance,
  isTspProblem,
  isVrpProblem,
}) {
  const problemParams = { ...baseProblemParams };

  if (isTspProblem) {
    problemParams.tspInstance = tspInstance;
  }

  if (isVrpProblem) {
    problemParams.vrpInstance = vrpInstance;
  }

  return problemParams;
}

// Build the searchSpaceParams object sent to the backend.
export function buildSearchSpaceParams({
  baseSearchSpaceParams,
  tspInstance,
  vrpInstance,
  isTspProblem,
  isVrpProblem,
}) {
  const searchSpaceParams = { ...baseSearchSpaceParams };

  if (isTspProblem) {
    searchSpaceParams.n = tspInstance.cities.length;
  }

  if (isVrpProblem) {
    searchSpaceParams.n = vrpInstance.customers.length;
  }

  return searchSpaceParams;
}

// Collect and validate the common data needed to start an execution.
export function buildExecutionContext({
  puzzleConfig,
  params,
  tspInstance,
  vrpInstance,
}) {
  const seed = params.global?.seed ?? Date.now();
  const existingSessionId = getExistingSessionId();
  const problemList = Array.isArray(puzzleConfig.problem) ? puzzleConfig.problem : [];

  const { isTspProblem, isVrpProblem } = validateProblemInstances({
    problemList,
    tspInstance,
    vrpInstance,
  });

  const problemParams = buildProblemParams({
    baseProblemParams: params.problem,
    tspInstance,
    vrpInstance,
    isTspProblem,
    isVrpProblem,
  });

  const searchSpaceParams = buildSearchSpaceParams({
    baseSearchSpaceParams: params.searchSpace,
    tspInstance,
    vrpInstance,
    isTspProblem,
    isVrpProblem,
  });

  return { seed,  existingSessionId,  problemParams,  searchSpaceParams, };
}

// Build the request payload for starting a runtime study.
export function buildRuntimeStudyRequest({
  studyId,
  sessionId,
  puzzleConfig,
  params,
  searchSpaceParams,
  problemParams,
  seed,
  problemSizes,
}) {
  return {
    studyId,
    sessionId,
    searchSpaceId: puzzleConfig.searchSpace?.[0]?.id ?? null,
    searchSpaceParams,
    problemId: puzzleConfig.problem?.[0]?.id ?? null,
    problemParams,
    generatorId: puzzleConfig.generator?.[0]?.id ?? null,
    generatorParams: params.generator,
    selectionRuleId: puzzleConfig.selection?.[0]?.id ?? null,
    selectionRuleParams: params.selection,
    populationModelId: puzzleConfig.populationModel?.[0]?.id ?? null,
    populationModelParams: params.populationModel,
    parentSelectionRuleId: puzzleConfig.parentSelectionRule?.[0]?.id ?? null,
    parentSelectionRuleParams: params.parentSelectionRule,
    crossoverId: puzzleConfig.crossover?.[0]?.id ?? null,
    crossoverParams: params.crossover,
    stopConditionIds: puzzleConfig.stopCondition?.map((x) => x.id) ?? [],
    stopConditionParams: params.stopCondition,
    seed,
    problemSizes,
    repetitionsPerSize: params.global?.repetitionsPerSize ?? 30,
    wsUpdateEverySizes: params.global?.wsUpdateEverySizes ?? 1,
  };
}

// Build the request payload for starting a standard run.
export function buildRunRequest({
  runId,
  sessionId,
  puzzleConfig,
  params,
  searchSpaceParams,
  problemParams,
  seed,
}) {
  return {
    searchSpaceId: puzzleConfig.searchSpace?.[0]?.id ?? null,
    searchSpaceParams,
    problemIds: puzzleConfig.problem?.map((x) => x.id) ?? [],
    problemParams,
    generatorId: puzzleConfig.generator?.[0]?.id ?? null,
    generatorParams: params.generator,
    selectionRuleId: puzzleConfig.selection?.[0]?.id ?? null,
    selectionRuleParams: params.selection,
    populationModelId: puzzleConfig.populationModel?.[0]?.id ?? null,
    populationModelParams: params.populationModel,
    parentSelectionRuleId: puzzleConfig.parentSelectionRule?.[0]?.id ?? null,
    parentSelectionRuleParams: params.parentSelectionRule,
    crossoverId: puzzleConfig.crossover?.[0]?.id ?? null,
    crossoverParams: params.crossover,
    stopConditionIds: puzzleConfig.stopCondition?.map((x) => x.id) ?? [],
    stopConditionParams: params.stopCondition,
    observerIds: puzzleConfig.observer?.map((x) => x.id) ?? [],
    observerParams: params.observer,
    seed,
    runTimes: params.global?.runTimes ?? 1,
    sessionId,
    runId,
    logEveryEvaluations: params.global?.logEveryEvaluations ?? 100,
    wsUpdateEveryEvaluations: params.global?.wsUpdateEveryEvaluations ?? 100,
  };
}