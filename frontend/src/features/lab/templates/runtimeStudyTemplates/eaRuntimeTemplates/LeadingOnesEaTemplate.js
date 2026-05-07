export const LeadingOnesEa = [
  {
    id: "ea-bitstring-leadingones-runtime",
    displayName: "(mu+lambda) EA on LeadingOnes",
    runtimeStudyRequest: {
      searchSpaceId: "bitstring",
      searchSpaceParams: {},

      problemId: "leadingones",
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
      problemSizes: [200, 400, 600, 800],
      repetitionsPerSize: 10,
    },
  },
];