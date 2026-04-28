export const bitstringEaTemplates = [
  {
    id: "tpl-ea-bitstring-onemax-leadingones",
    displayName: "(mu+lambda) EA on OneMax + LeadingOnes",
    runRequest: {
      searchSpaceId: ["bitstring"],
      searchSpaceParams: { n: 100 },

      problemIds: ["onemax", "leadingones"],
      problemParams: {},

      generatorId: ["bit-flip"],
      generatorParams: { flipProbability: "1/n" },

      selectionRuleId: ["mu-plus-lambda"],
      selectionRuleParams: {},

      parentSelectionRuleId: ["elitist-parents"],
      parentSelectionRuleParams: {},

      populationModelId: ["mu-lambda"],
      populationModelParams: { mu: 1, lambda: 1 },

      stopConditionIds: ["max-iterations", "optimum-reached"],
      stopConditionParams: { maxIterations: 10000 },

      observerIds: ["fitness", "fitness-phase", "hypercube"],

      seed: 1,
      runTimes: 10,
    },
  },
];