package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.util.InstanceMapper;
import dk.dtu.scout.datatypes.TSPInstance;
import dk.dtu.scout.datatypes.VRPInstance;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class InstanceService {

    public String exportInstance(String exportType, Map<String, Object> payload) {
        if (exportType == null || exportType.isBlank()) {
            throw new IllegalArgumentException("exportType must be provided");
        }

        Map<String, Object> sanitized = payload == null ? Map.of() : new LinkedHashMap<>(payload);
        sanitized.remove("exportType");

        String normalized = exportType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "TSP" -> formatTsp(InstanceMapper.toTspInstance(sanitized));
            case "VRP" -> formatVrp(InstanceMapper.toVrpInstance(sanitized));
            default -> throw new IllegalArgumentException("exportType must be TSP or VRP");
        };
    }

    private String formatTsp(TSPInstance instance) {
        StringBuilder out = new StringBuilder();
        String name = normalizeName(instance.getName(), "Custom TSP Instance");
        out.append("NAME: ").append(name).append("\n");
        out.append("TYPE: TSP\n");
        out.append("DIMENSION: ").append(instance.getDimension()).append("\n");
        out.append("EDGE_WEIGHT_TYPE: EUC_2D\n");
        out.append("NODE_COORD_SECTION\n");

        double[][] coords = instance.getCoordinates();
        for (int i = 0; i < coords.length; i++) {
            out.append(i + 1)
                .append(" ")
                .append(formatNumber(coords[i][0]))
                .append(" ")
                .append(formatNumber(coords[i][1]))
                .append("\n");
        }

        out.append("EOF\n");
        return out.toString();
    }

    private String formatVrp(VRPInstance instance) {
        StringBuilder out = new StringBuilder();
        String name = normalizeName(instance.getName(), "Custom VRP Instance");
        name = ensureVehicleSuffix(name, instance.getNumberOfVehicles());

        int dimension = instance.getCustomerCount() + 1;
        out.append("NAME: ").append(name).append("\n");
        out.append("TYPE: CVRP\n");
        out.append("DIMENSION: ").append(dimension).append("\n");
        out.append("EDGE_WEIGHT_TYPE: EUC_2D\n");
        out.append("CAPACITY: ").append(formatNumber(instance.getCapacity())).append("\n");
        out.append("NODE_COORD_SECTION\n");

        double[] depot = instance.getDepotCoordinates();
        out.append("1 ")
            .append(formatNumber(depot[0]))
            .append(" ")
            .append(formatNumber(depot[1]))
            .append("\n");

        double[][] customers = instance.getCustomerCoordinates();
        for (int i = 0; i < customers.length; i++) {
            out.append(i + 2)
                .append(" ")
                .append(formatNumber(customers[i][0]))
                .append(" ")
                .append(formatNumber(customers[i][1]))
                .append("\n");
        }

        out.append("DEMAND_SECTION\n");
        out.append("1 0\n");
        for (int i = 0; i < customers.length; i++) {
            out.append(i + 2)
                .append(" ")
                .append(formatNumber(instance.getDemand(i)))
                .append("\n");
        }

        out.append("DEPOT_SECTION\n");
        out.append("1\n");
        out.append("-1\n");
        out.append("EOF\n");
        return out.toString();
    }

    private String normalizeName(String name, String fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        return name.trim();
    }

    private String ensureVehicleSuffix(String name, int vehicleCount) {
        if (vehicleCount > 0 && !name.matches(".*-k\\d+.*")) {
            return name + "-k" + vehicleCount;
        }
        return name;
    }

    private String formatNumber(double value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat formatter = new DecimalFormat("0.######", symbols);
        formatter.setGroupingUsed(false);
        return formatter.format(value);
    }
}
