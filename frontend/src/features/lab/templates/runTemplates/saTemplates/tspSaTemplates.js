export const tspSaTemplates = [
  {
    id: "tpl-sa-tsp",
    displayName: "Simulated Annealing on TSP",
    runRequest: {
      searchSpaceId: ["permutation"],
      searchSpaceParams: {},

      problemIds: ["tsp"],
      problemParams: {
        tspInstance: null,
      },

      generatorId: ["2opt"],
      generatorParams: {},

      selectionRuleId: ["annealed-selection"],
      selectionRuleParams: {
        initialTemperature: 5.0,
        coolingRate: 0.999,
        minTemperature: 0.000001,
      },

      parentSelectionRuleId: ["random-parents"],
      parentSelectionRuleParams: {},

      populationModelId: ["mu-lambda"],
      populationModelParams: { lambda: 1 },

      stopConditionIds: ["max-evaluations"],
      stopConditionParams: { maxEvaluations: 10000 },

      observerIds: ["fitness", "fitness-phase", "acceptance-rate", "tour"],

      seed: 1,
      runTimes: 10,
    },
  },
];