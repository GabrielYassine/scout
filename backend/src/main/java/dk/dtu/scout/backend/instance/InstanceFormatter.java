package dk.dtu.scout.backend.instance;

import dk.dtu.scout.datatypes.TSPInstance;
import dk.dtu.scout.datatypes.VRPInstance;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formatter for TSP and VRP instances.
 * Converts TSPInstance and VRPInstance objects into their respective .tsp and .vrp file formats for export.
 * Ensures that names, comments, coordinates, demands, and capacities are formatted consistently for supported TSPLIB/VRPLIB-style exports.
 * @author s235257
 */
public final class InstanceFormatter {

    private static final String EDGE_WEIGHT_TYPE = "EUC_2D";
    private static final DecimalFormatSymbols DECIMAL_SYMBOLS = new DecimalFormatSymbols(Locale.US);

    private InstanceFormatter() {
    }

    /**
     * Formats a TSP instance into the standard .tsp file format.
     * @param instance the TSP instance to format
     * @return a string containing the TSP instance in .tsp file format
     */
    public static String formatTsp(TSPInstance instance) {
        StringBuilder out = new StringBuilder();

        appendHeader(out, normalizeName(instance.getName(), "Custom TSP Instance"), "TSP", instance.getComment());

        out.append("DIMENSION: ").append(instance.getDimension()).append("\n");
        out.append("EDGE_WEIGHT_TYPE: ").append(EDGE_WEIGHT_TYPE).append("\n");
        out.append("NODE_COORD_SECTION\n");

        double[][] coordinates = instance.getCoordinates();
        for (int i = 0; i < coordinates.length; i++) {
            appendCoordinate(out, i + 1, coordinates[i][0], coordinates[i][1]);
        }

        out.append("EOF\n");
        return out.toString();
    }

    /**
     * Formats a VRP instance into the standard .vrp file format.
     * @param instance the VRP instance to format
     * @return a string containing the VRP instance in .vrp file format
     */
    public static String formatVrp(VRPInstance instance) {
        StringBuilder out = new StringBuilder();

        appendHeader(out, normalizeName(instance.getName(), "Custom VRP Instance"), "CVRP", instance.getComment());

        out.append("DIMENSION: ").append(instance.getCustomerCount() + 1).append("\n");
        out.append("EDGE_WEIGHT_TYPE: ").append(EDGE_WEIGHT_TYPE).append("\n");
        out.append("CAPACITY: ").append(formatInteger(instance.getCapacity())).append("\n");
        out.append("NODE_COORD_SECTION\n");

        double[] depot = instance.getDepotCoordinates();
        appendCoordinate(out, 1, depot[0], depot[1]);

        double[][] customers = instance.getCustomerCoordinates();
        for (int i = 0; i < customers.length; i++) {
            appendCoordinate(out, i + 2, customers[i][0], customers[i][1]);
        }

        out.append("DEMAND_SECTION\n");
        out.append("1 0\n");

        for (int i = 0; i < customers.length; i++) {
            out.append(i + 2)
                .append(" ")
                .append(formatInteger(instance.getDemand(i)))
                .append("\n");
        }

        out.append("DEPOT_SECTION\n");
        out.append("1\n");
        out.append("-1\n");
        out.append("EOF\n");

        return out.toString();
    }

    private static void appendHeader(StringBuilder out, String name, String type, String comment) {
        out.append("NAME: ").append(name).append("\n");
        out.append("TYPE: ").append(type).append("\n");

        String cleanedComment = cleanComment(comment);
        if (cleanedComment != null) {
            out.append("COMMENT: ").append(cleanedComment).append("\n");
        }
    }

    private static void appendCoordinate(StringBuilder out, int nodeId, double x, double y) {
        out.append(nodeId)
            .append(" ")
            .append(formatNumber(x))
            .append(" ")
            .append(formatNumber(y))
            .append("\n");
    }

    private static String normalizeName(String name, String fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }

        return name.trim();
    }

    private static String cleanComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }

        return comment.trim().replaceAll("\\s+", " ");
    }

    private static String formatNumber(double value) {
        DecimalFormat formatter = new DecimalFormat("0.##", DECIMAL_SYMBOLS);
        formatter.setGroupingUsed(false);
        return formatter.format(value);
    }

    private static String formatInteger(double value) {
        return Long.toString(Math.round(value));
    }
}