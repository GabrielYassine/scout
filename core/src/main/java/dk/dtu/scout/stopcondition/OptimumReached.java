package dk.dtu.scout.stopcondition;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.State;
import dk.dtu.scout.StateKeys;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.problems.TSPProblem;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Component
@Scope("prototype")
public class OptimumReached<S> implements StopCondition<S> {

    private static final String TSP_OPTIMA_RESOURCE = "optima/tsp-optima.properties";
    private static final double EPSILON = 1e-9;
    private static final Map<String, Double> TSP_OPTIMA = loadTspOptima();

    private Problem<?> problem;

    public OptimumReached() {}

    public void setProblem(Problem<?> problem) {
        this.problem = problem;
    }

    @Override
    public String id() {
        return "optimum-reached";
    }

    @Override
    public String displayName() {
        return "Optimum Reached";
    }

    @Override
    public String description() {
        return "Stops when the optimal solution is found";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public List<String> supportedSearchSpaces() {
        return List.of("bitstring", "permutation");
    }

    @Override
    public void init(State state) {
        if (state == null) {
            return;
        }
        Object problemObj = state.get(StateKeys.PROBLEM);
        if (problemObj instanceof Problem<?> p) {
            this.problem = p;
        }
    }

    @Override
    public boolean shouldStop(int iteration, int evaluations, double bestFitness, S bestSolution) {
        if (problem instanceof TSPProblem tspProblem) {
            Double optimum = resolveTspOptimum(tspProblem);
            return optimum != null && bestFitness >= -optimum - EPSILON;
        }

        return problem != null && problem.isOptimal(bestFitness);
    }

    private static Double resolveTspOptimum(TSPProblem tspProblem) {
        double configured = tspProblem.getOptimalTourLength();
        if (configured > 0.0) {
            return configured;
        }

        if (tspProblem.getInstance() == null) {
            return null;
        }

        String normalizedName = normalizeInstanceName(tspProblem.getInstance().getName());
        if (normalizedName == null) {
            return null;
        }

        return TSP_OPTIMA.get(normalizedName);
    }

    private static String normalizeInstanceName(String name) {
        if (name == null) {
            return null;
        }

        String cleaned = name.trim().toLowerCase();

        if (cleaned.endsWith(".tsp")) {
            cleaned = cleaned.substring(0, cleaned.length() - 4);
        }

        cleaned = cleaned.replaceAll("[^a-z0-9]", "");

        return cleaned.isBlank() ? null : cleaned;
    }

    private static Map<String, Double> loadTspOptima() {
        Map<String, Double> result = new HashMap<>();

        try (InputStream stream = OptimumReached.class.getClassLoader()
                .getResourceAsStream(TSP_OPTIMA_RESOURCE)) {

            if (stream == null) {
                return result;
            }

            Properties props = new Properties();
            props.load(stream);

            for (String key : props.stringPropertyNames()) {
                String normalizedKey = normalizeInstanceName(key);
                String value = props.getProperty(key);

                if (normalizedKey == null || value == null) {
                    continue;
                }

                try {
                    result.put(normalizedKey, Double.parseDouble(value.trim()));
                } catch (NumberFormatException ignored) {
                    // Skip malformed entries
                }
            }
        } catch (Exception ignored) {
            // Return whatever was loaded
        }

        return result;
    }
}