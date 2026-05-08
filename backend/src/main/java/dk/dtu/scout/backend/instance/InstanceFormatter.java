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

    /**
     * Appends the common header section for both TSP and VRP formats, including the name, type, and comment fields.
     * @param out the StringBuilder to append to
     * @param name the name of the instance, which will be normalized to a non-blank string
     * @param type  the type of the instance, such as "TSP" or "CVRP"
     * @param comment the comment for the instance, which will be cleaned to remove extra whitespace and can be null if blank
     */
    private static void appendHeader(StringBuilder out, String name, String type, String comment) {
        out.append("NAME: ").append(name).append("\n");
        out.append("TYPE: ").append(type).append("\n");

        String cleanedComment = cleanComment(comment);
        if (cleanedComment != null) {
            out.append("COMMENT: ").append(cleanedComment).append("\n");
        }
    }

    /**
     * Appends a line for a node's coordinates in the format "nodeId x y", where x and y are formatted to have up to 2 decimal places.
     * @param out   the StringBuilder to append to
     * @param nodeId  the 1-based index of the node (e.g., city or customer), which is used in the output file to identify the node
     * @param x the x-coordinate of the node, which will be formatted to a string with up to 2 decimal places
     * @param y the y-coordinate of the node, which will be formatted to a string with up to 2 decimal places
     */
    private static void appendCoordinate(StringBuilder out, int nodeId, double x, double y) {
        out.append(nodeId)
            .append(" ")
            .append(formatNumber(x))
            .append(" ")
            .append(formatNumber(y))
            .append("\n");
    }

    /**
     * Normalizes the name by trimming whitespace and ensuring it is not blank. If the input name is blank, the provided fallback string is used instead.
     * @param name the name to normalize, which will be trimmed and checked for blankness
     * @param fallback the fallback string to use if the input name is blank, ensuring that the output always has a valid name field
     * @return the normalized name, which is guaranteed to be non-blank and trimmed of extra whitespace
     */
    private static String normalizeName(String name, String fallback) {
        if (name.isBlank()) {
            return fallback;
        }
        return name.trim();
    }

    /**
     * Cleans the comment string by trimming whitespace and replacing multiple consecutive whitespace characters with a single space.
     * @param comment
     * @return the cleaned comment string, or null if the input is blank
     */
    private static String cleanComment(String comment) {
        if (comment.isBlank()) {
            return null;
        }
        return comment.trim().replaceAll("\\s+", " ");
    }

    /**
     * Formats a double value to a string with up to 2 decimal places, using '.' as the decimal separator and no grouping.
     * This ensures consistent formatting for coordinates and capacities in the output files.
     * @param value the double value to format
     * @return a string representation of the value with up to 2 decimal places
     */
    private static String formatNumber(double value) {
        DecimalFormat formatter = new DecimalFormat("0.##", DECIMAL_SYMBOLS);
        formatter.setGroupingUsed(false);
        return formatter.format(value);
    }

    private static String formatInteger(double value) {
        return Long.toString(Math.round(value));
    }
}