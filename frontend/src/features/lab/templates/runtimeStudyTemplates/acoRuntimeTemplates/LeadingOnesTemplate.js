export const LeadingOnesAco = [
  {
    id: "aco-bitstring-leadingones-runtime",
    displayName: "ACO on LeadingOnes",
    runtimeStudyRequest: {
      searchSpaceId: "bitstring",
      searchSpaceParams: {},

      problemId: "leadingones",
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
      problemSizes: [200, 400, 600, 800],
      repetitionsPerSize: 10,
    },
  },
];