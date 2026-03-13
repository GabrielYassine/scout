package dk.dtu.scout.construction;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.heuristic.HeuristicFunction;
import dk.dtu.scout.pheromone.PheromoneModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
public class AntSystemConstruction implements ConstructionPolicy<int[]> {

    private double alpha = 1.0;  // Pheromone influence
    private double beta = 2.0;   // Heuristic influence

    @Override
    public String id() {
        return "ant-system";
    }

    @Override
    public String displayName() {
        return "Ant System Construction";
    }

    @Override
    public String description() {
        return "Probabilistic construction using pheromone and heuristic information";
    }

    @Override
    public List<Parameter> params() {
        return List.of(
            new Parameter("alpha", "Pheromone influence (α)", "double", alpha, 0.0, null),
            new Parameter("beta", "Heuristic influence (β)", "double", beta, 0.0, null)
        );
    }

    @Override
    public List<String> supportedSearchSpaces() {
        return List.of("permutation");
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) return;
        if (params.containsKey("alpha")) {
            this.alpha = ((Number) params.get("alpha")).doubleValue();
        }
        if (params.containsKey("beta")) {
            this.beta = ((Number) params.get("beta")).doubleValue();
        }
    }

    @Override
    public int[] constructSolution(PheromoneModel<int[]> pheromones, HeuristicFunction<int[]> heuristics, Random rng) {
        int dimension = pheromones.getDimension();
        int[] solution = new int[dimension];
        boolean[] visited = new boolean[dimension];

        // Start from a random city
        int current = rng.nextInt(dimension);
        solution[0] = current;
        visited[current] = true;

        // Build the rest of the tour
        for (int step = 1; step < dimension; step++) {
            int next = selectNextCity(current, visited, pheromones, heuristics, rng);
            solution[step] = next;
            visited[next] = true;
            current = next;
        }

        return solution;
    }

    private int selectNextCity(int current, boolean[] visited, PheromoneModel<int[]> pheromones,
                               HeuristicFunction<int[]> heuristics, Random rng) {
        int dimension = visited.length;
        List<Integer> candidates = new ArrayList<>();
        List<Double> probabilities = new ArrayList<>();
        double totalProb = 0.0;

        // Calculate probabilities for all unvisited cities
        for (int i = 0; i < dimension; i++) {
            if (!visited[i]) {
                double pheromone = Math.pow(pheromones.getPheromone(current, i), alpha);
                double heuristic = Math.pow(heuristics.getHeuristic(current, i), beta);
                double prob = pheromone * heuristic;

                candidates.add(i);
                probabilities.add(prob);
                totalProb += prob;
            }
        }

        if (candidates.isEmpty()) {
            throw new IllegalStateException("No unvisited cities available");
        }

        // Handle case where all probabilities are zero
        if (totalProb < 1e-10) {
            return candidates.get(rng.nextInt(candidates.size()));
        }

        // Roulette wheel selection
        double rand = rng.nextDouble() * totalProb;
        double cumulative = 0.0;

        for (int i = 0; i < candidates.size(); i++) {
            cumulative += probabilities.get(i);
            if (cumulative >= rand) {
                return candidates.get(i);
            }
        }

        return candidates.get(candidates.size() - 1);
    }
}
