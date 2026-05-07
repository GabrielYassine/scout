export const OneMaxAco = [
  {
    id: "aco-bitstring-onemax-runtime",
    displayName: "ACO on OneMax",
    runtimeStudyRequest: {
      searchSpaceId: "bitstring",
      searchSpaceParams: {},

      problemId: "onemax",
      problemParams: {},

      generatorId: ["bitstring-aco"],
      generatorParams: {
         evaporationRate: 0.1,
         alpha: 1.0,
         beta: 2.0,
      },

      selectionRuleId: ["mu-plus-lambda"],
      selectionRuleParams: {},

      parentSelectionRuleId: "random-parents",
      parentSelectionRuleParams: {},

      populationModelId: "mu-lambda",
      populationModelParams: { lambda: 10 },

      crossoverId: null,
      crossoverParams: {},

      stopConditionIds: ["optimum-reached"],
      stopConditionParams: {},

      seed: 1,
      problemSizes: [500, 1000, 1500, 2000, 2500],
      repetitionsPerSize: 10,
    },
  },
];