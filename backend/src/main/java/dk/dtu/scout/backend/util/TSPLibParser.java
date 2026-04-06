package dk.dtu.scout.backend.util;

import dk.dtu.scout.datatypes.TSPInstance;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Expected Format:
 * NAME: <instance name>
 * TYPE: TSP
 * COMMENT: <optional comment>
 * DIMENSION: <number of nodes>
 * EDGE_WEIGHT_TYPE: EUC_2D
 * NODE_COORD_SECTION
 * <city_index> <x_coordinate> <y_coordinate>
 * .......
 * EOF
 */

public class TSPLibParser {

    public static TSPInstance parse(String content) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(content));

        String name = null;
        String type = null;
        Integer dimension = null;
        String edgeWeightType = null;
        List<double[]> coordinates = new ArrayList<>();
        String line;

        // Parse header section
        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("NAME:")) {
                name = line.substring(5).trim();
            } else if (line.startsWith("TYPE:")) {
                type = line.substring(5).trim();
            } else if (line.startsWith("DIMENSION:")) {
                dimension = Integer.parseInt(line.substring(10).trim());
            } else if (line.startsWith("EDGE_WEIGHT_TYPE:")) {
                edgeWeightType = line.substring(17).trim();
            } else if (line.equals("NODE_COORD_SECTION")) {
                break;
            }
        }

        // Validate required fields
        if (name == null) {
            throw new IllegalArgumentException("Missing required field: NAME");
        }
        if (dimension == null) {
            throw new IllegalArgumentException("Missing required field: DIMENSION");
        }
        if (!"TSP".equals(type)) {
            throw new IllegalArgumentException("Expected TYPE: TSP, but got: " + type);
        }
        if (!"EUC_2D".equals(edgeWeightType)) {
            throw new IllegalArgumentException("Only EUC_2D edge weight type is supported, but got: " + edgeWeightType);
        }

        // Parse coordinate section
        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            // The coordinate section ends with "EOF"
            if (line.equals("EOF")) {
                break;
            }

            String[] parts = line.split("\\s+");
            if (parts.length < 3) {
                throw new IllegalArgumentException("Invalid coordinate line: " + line);
            }

            // parts[0] is the city index (1-based), parts[1] is x, parts[2] is y
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            coordinates.add(new double[]{x, y});
        }

        // Validate coordinate count
        if (coordinates.size() != dimension) {
            throw new IllegalArgumentException("Expected " + dimension + " coordinates, but got " + coordinates.size());
        }

        // Convert list to array
        double[][] coordArray = new double[coordinates.size()][2];
        for (int i = 0; i < coordinates.size(); i++) {
            coordArray[i] = coordinates.get(i);
        }

        return new TSPInstance(name, dimension, coordArray);
    }
}