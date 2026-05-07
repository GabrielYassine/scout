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

    public static Double resolve(Map<String, Double> optima, String instanceName) {
        return optima.get(normalizeInstanceKey(instanceName));
    }
}