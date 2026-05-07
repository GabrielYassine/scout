export const LeadingOnesSa = [
  {
    id: "sa-bitstring-leadingones-runtime",
    displayName: "Simulated Annealing on LeadingOnes",
    runtimeStudyRequest: {
      searchSpaceId: "bitstring",
      searchSpaceParams: {},

      problemId: "leadingones",
      problemParams: {},

      generatorId: "single-bit-flip",
      generatorParams: {},

      selectionRuleId: "annealed-selection",
      selectionRuleParams: {
        initialTemperature: 500.0,
        coolingRate: 0.99,
        minTemperature: 0.000001,
      },

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