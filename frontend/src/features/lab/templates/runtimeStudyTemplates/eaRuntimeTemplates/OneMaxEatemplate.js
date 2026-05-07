export const OneMaxEa= [
  {
    id: "ea-bitstring-onemax-runtime",
    displayName: "(mu+lambda) EA on OneMax",
    runtimeStudyRequest: {
      searchSpaceId: "bitstring",
      searchSpaceParams: {},

      problemId: "onemax",
      problemParams: {},

      generatorId: ["bit-flip"],
      generatorParams: { flipProbability: "1/n" },

      selectionRuleId: ["mu-plus-lambda"],
      selectionRuleParams: {},

      parentSelectionRuleId: "random-parents",
      parentSelectionRuleParams: {},

      populationModelId: "mu-lambda",
      populationModelParams: { lambda: 1 },

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