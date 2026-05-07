export const tspAcoTemplates = [
  {
    id: "aco-permutation-tsp",
    displayName: "Ant colony optimization on TSP",
    runRequest: {
      searchSpaceId: ["permutation"],
      searchSpaceParams: {},

      problemIds: ["tsp"],
      problemParams: {
        tspInstance: null,
      },

      generatorId: ["tsp-aco"],
      generatorParams: {
        evaporationRate: 0.1,
        alpha: 1.0,
        beta: 2.0,
      },

      selectionRuleId: ["mu-plus-lambda"],
      selectionRuleParams: {},

      parentSelectionRuleId: ["random-parents"],
      parentSelectionRuleParams: {},

      populationModelId: ["mu-lambda"],
      populationModelParams: { lambda: 10 },

      stopConditionIds: ["max-iterations"],
      stopConditionParams: { maxIterations: 10000 },

      observerIds: ["tour"],

      seed: 1,
      runTimes: 1,
    },
  },
];