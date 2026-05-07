package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.instance.InstanceFormatter;
import dk.dtu.scout.backend.instance.InstanceMapper;
import dk.dtu.scout.backend.instance.InstanceParser;
import dk.dtu.scout.datatypes.TSPInstance;
import dk.dtu.scout.datatypes.VRPInstance;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Service for handling instance import and export.
 * Responsible for coordinating parsing, normalization, and formatting of TSP and VRP instances.
 * @author s235257
 */
@Service
public class InstanceService {

    /**
     * Handles files of type .vrp and .tsp and converts them into a JSON format for frontend display and run execution.
     * @param content the content of the instance file to import
     * @return a map containing the instance type and a normalized instance payload ready for use in run execution
     */
    public Map<String, Object> importInstance(String content) {
        if (content.isBlank()) {
            throw new IllegalArgumentException("Instance file content must be provided");
        }

        String instanceType = InstanceParser.detectInstanceType(content);

        return switch (instanceType) {
            case "VRP" -> {
                Map<String, Object> parsed = InstanceParser.parseVrpContent(content);
                InstanceMapper.toVrpInstance(parsed);
                yield Map.of("instanceType", "VRP", "instance", parsed);
            }
            case "TSP" -> {
                Map<String, Object> parsed = InstanceParser.parseTspContent(content);
                InstanceMapper.toTspInstance(parsed);
                yield Map.of("instanceType", "TSP", "instance", parsed);
            }
            default -> throw new IllegalArgumentException("Unsupported instance type: " + instanceType);
        };
    }

    /**
     * Converts a TSP or VRP instance from the frontend format back into the standard .tsp or .vrp file format for export.
     * @param payload the instance data in frontend format, including exportType, which will be normalized and validated before export
     * @return a string containing the instance in the appropriate file format for download
     */
    public String exportInstance(Map<String, Object> payload) {
        Object rawExportType = payload.get("exportType");
        if (rawExportType == null || rawExportType.toString().isBlank()) {
            throw new IllegalArgumentException("exportType must be provided");
        }

        Map<String, Object> instanceData = new LinkedHashMap<>(payload);
        instanceData.remove("exportType");

        String normalizedType = rawExportType.toString().trim().toUpperCase(Locale.ROOT);

        return switch (normalizedType) {
            case "TSP" -> {
                TSPInstance instance = InstanceMapper.toTspInstance(instanceData);
                yield InstanceFormatter.formatTsp(instance);
            }
            case "VRP" -> {
                VRPInstance instance = InstanceMapper.toVrpInstance(instanceData);
                yield InstanceFormatter.formatVrp(instance);
            }
            default -> throw new IllegalArgumentException("Unsupported exportType: " + rawExportType);
        };
    }
}