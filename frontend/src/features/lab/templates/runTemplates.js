export const runTemplates = [
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

  {
    id: "tpl-sa-bitstring-onemax-leadingones",
    displayName: "Simulated Annealing on OneMax + LeadingOnes",
    runRequest: {
      searchSpaceId: ["bitstring"],
      searchSpaceParams: { n: 100 },

      problemIds: ["onemax", "leadingones"],
      problemParams: {},

      generatorId: ["single-bit-flip"],
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

      stopConditionIds: ["max-iterations", "optimum-reached"],
      stopConditionParams: { maxIterations: 10000 },

      observerIds: ["fitness", "fitness-phase", "acceptance-rate", "hypercube"],

      seed: 1,
      runTimes: 10,
    },
  },

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

      stopConditionIds: ["max-iterations"],
      stopConditionParams: { maxIterations: 10000 },

      observerIds: ["fitness", "fitness-phase", "acceptance-rate", "tour"],

      seed: 1,
      runTimes: 10,
    },
  },

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
      populationModelParams: {
        lambda: 1,
      },

      stopConditionIds: ["max-iterations"],
      stopConditionParams: {
        maxIterations: 10000,
      },

      observerIds: ["fitness", "fitness-phase", "acceptance-rate", "tour"],

      seed: 1,
      runTimes: 10,
    },
  },

  {
    id: "tpl-ea-vrp",
    displayName: "(mu+lambda) EA on VRP",
    description: "Baseline EA configuration for CVRP with route-list encoding",
    runRequest: {
      searchSpaceId: ["route-list"],
      searchSpaceParams: {
        vrpInstance: null,
      },

      problemIds: ["vrp"],
      problemParams: {
        vrpInstance: null,
      },

      generatorId: ["route-list-relocate"],
      generatorParams: {},

      selectionRuleId: ["mu-plus-lambda"],
      selectionRuleParams: {},

      parentSelectionRuleId: ["random-parents"],
      parentSelectionRuleParams: {},

      populationModelId: ["mu-lambda"],
      populationModelParams: { mu: 1, lambda: 1 },

      stopConditionIds: ["max-iterations"],
      stopConditionParams: { maxIterations: 10000 },

      observerIds: ["tour"],

      seed: 1,
      runTimes: 1,
    },
  },

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
      populationModelParams: { lambda: 3 },

      stopConditionIds: ["max-iterations"],
      stopConditionParams: { maxIterations: 10000 },

      observerIds: ["tour"],

      seed: 1,
      runTimes: 1,
    },
  },

  {
    id: "aco-bitstring",
    displayName: "Ant colony optimization on bitstrings",
    runRequest: {
      searchSpaceId: ["bitstring"],
      searchSpaceParams: { n: 100 },

      problemIds: ["onemax"],
      problemParams: {},

      generatorId: ["bitstring-aco"],
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

      stopConditionIds: ["max-iterations", "optimum-reached"],
      stopConditionParams: { maxIterations: 10000 },

      observerIds: ["fitness"],

      seed: 1,
      runTimes: 1,
    },
  },
];