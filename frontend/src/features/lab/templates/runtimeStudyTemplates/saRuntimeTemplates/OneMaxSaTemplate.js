export const OneMaxSa = [
  {
    id: "sa-bitstring-onemax-runtime",
    displayName: "Simulated Annealing on OneMax",
    runtimeStudyRequest: {
      searchSpaceId: "bitstring",
      searchSpaceParams: {},

      problemId: "onemax",
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
      problemSizes: [100, 200, 400, 800],
      repetitionsPerSize: 10,
    },
  },
];