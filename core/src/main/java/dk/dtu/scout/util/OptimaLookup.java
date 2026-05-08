package dk.dtu.scout.util;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Utility class for loading known optimal values for problem instances from resource files.
 * @author s235257
 */
public final class OptimaLookup {

    private OptimaLookup() {}
    /**
     * Loads a map of instance names to their known optimal values from a properties file in the resources.
     * @param resourcePath Path to the properties file in the resources (e.g., "optima/tsp_optima.properties")
     * @return A map of normalized instance names to their known optimal values
     */
    public static Map<String, Double> loadDoubleMap(String resourcePath) {
        Map<String, Double> result = new HashMap<>();

        try (InputStream stream = OptimaLookup.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return result;
            }

            Properties props = new Properties();
            props.load(stream);

            for (String key : props.stringPropertyNames()) {
                String normalizedKey = normalizeInstanceKey(key);
                String value = props.getProperty(key).trim();

                try {
                    result.put(normalizedKey, Double.parseDouble(value));
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (Exception ignored) {
        }

        return result;
    }
    /**
     * Normalizes an instance name by trimming whitespace, converting to lowercase, and removing file extensions like .tsp or .vrp.
     * @param name The original instance name
     * @return A normalized instance key suitable for lookup
     */
    public static String normalizeInstanceKey(String name) {
        if (name == null) {
            return "";
        }

        String key = name.trim().toLowerCase();

        if (key.endsWith(".tsp") || key.endsWith(".vrp")) {
            key = key.substring(0, key.lastIndexOf('.'));
        }

        return key;
    }
    /**
     * Resolves the known optimal value for a given instance name using the provided map of optima.
     * @param optima Map of normalized instance names to their known optimal values
     * @param instanceName The original instance name to look up
     * @return The known optimal value for the instance, or null if not found
     */
    public static Double resolve(Map<String, Double> optima, String instanceName) {
        return optima.get(normalizeInstanceKey(instanceName));
    }
}