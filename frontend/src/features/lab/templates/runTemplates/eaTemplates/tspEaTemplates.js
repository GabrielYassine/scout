export const tspEaTemplates = [
  {
    id: "tpl-ea-tsp",
    displayName: "(mu+lambda) EA on TSP",
    runRequest: {
      searchSpaceId: ["permutation"],
      searchSpaceParams: {},

      problemIds: ["tsp"],
      problemParams: {
        tspInstance: null,
      },

      generatorId: ["2opt"],
      generatorParams: {},

      selectionRuleId: ["mu-plus-lambda"],
      selectionRuleParams: {},

      parentSelectionRuleId: ["random-parents"],
      parentSelectionRuleParams: {},

      populationModelId: ["mu-lambda"],
      populationModelParams: { mu: 1, lambda: 1 },

      stopConditionIds: ["max-evaluations"],
      stopConditionParams: { maxEvaluations: 10000 },

      observerIds: ["fitness", "fitness-phase", "acceptance-rate", "tour"],

      seed: 1,
      runTimes: 10,
    },
  },
];