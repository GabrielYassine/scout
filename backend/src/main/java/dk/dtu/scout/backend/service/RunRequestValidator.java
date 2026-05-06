package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.request.RunRequest;
import dk.dtu.scout.backend.dto.request.RuntimeStudyRequest;
import dk.dtu.scout.backend.exception.BadRequestException;
import dk.dtu.scout.backend.instance.InstanceMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Validates run requests, that includes global settings, components, their parameters, and problem instances.
 * @author s235257 @ Ahmed
 */
@Service
public class RunRequestValidator {

    /**
     * Validates for a standard run request that all required fields are present and well-formed.
     * @param request the run request to validate, which should contain all settings.
     */
    public void validateRunRequest(RunRequest request) {
        if (request.searchSpaceId() == null || request.searchSpaceId().isBlank()) {
            throw new BadRequestException("Search space must be specified");
        }
        if (request.problemIds() == null || request.problemIds().isEmpty()) {
            throw new BadRequestException("Problem must be specified");
        }
        if (request.generatorId() == null || request.generatorId().isBlank()) {
            throw new BadRequestException("Generator must be specified");
        }
        if (request.populationModelId() == null || request.populationModelId().isBlank()) {
            throw new BadRequestException("Population model must be specified");
        }
        if (request.selectionRuleId() == null || request.selectionRuleId().isBlank()) {
            throw new BadRequestException("Selection rule must be specified");
        }
        if (request.parentSelectionRuleId() == null || request.parentSelectionRuleId().isBlank()) {
            throw new BadRequestException("Parent selection rule must be specified");
        }
        if (request.stopConditionIds() == null || request.stopConditionIds().isEmpty()) {
            throw new BadRequestException("Stop condition must be specified");
        }
        if (request.runTimes() <= 0) {
            throw new BadRequestException("runTimes must be positive");
        }
        if (request.logEveryIterations() < 0) {
            throw new BadRequestException("logEveryIterations must be zero or positive");
        }

        validateProblemInstances(request.problemIds(), request.problemParams());
    }

    /**
     * Validates that the provided runtime study request contains all required fields,
     * that they are well-formed, and that the request only uses supported runtime-study configurations.
     * @param request the runtime study request to validate
     */
    public void validateRuntimeStudyRequest(RuntimeStudyRequest request) {
        if (request.searchSpaceId() == null || request.searchSpaceId().isBlank()) {
            throw new BadRequestException("Search space must be specified");
        }
        if (request.problemId() == null || request.problemId().isBlank()) {
            throw new BadRequestException("Problem must be specified");
        }

        if ("tsp".equals(request.problemId()) || "vrp".equals(request.problemId())) {
            throw new BadRequestException("Runtime study currently supports theoretical size-based problems only");
        }

        if (request.generatorId() == null || request.generatorId().isBlank()) {
            throw new BadRequestException("Generator must be specified");
        }
        if (request.populationModelId() == null || request.populationModelId().isBlank()) {
            throw new BadRequestException("Population model must be specified");
        }
        if (request.selectionRuleId() == null || request.selectionRuleId().isBlank()) {
            throw new BadRequestException("Selection rule must be specified");
        }
        if (request.parentSelectionRuleId() == null || request.parentSelectionRuleId().isBlank()) {
            throw new BadRequestException("Parent selection rule must be specified");
        }
        if (request.stopConditionIds() == null || request.stopConditionIds().isEmpty()
                || !request.stopConditionIds().contains("optimum-reached")) {
            throw new BadRequestException("Stop condition must contain 'optimum-reached' and cannot be empty");
        }
        if (request.problemSizes() == null || request.problemSizes().isEmpty()) {
            throw new BadRequestException("At least one problem size must be specified");
        }
        if (request.problemSizes().stream().anyMatch(n -> n == null || n <= 0)) {
            throw new BadRequestException("All problem sizes must be positive");
        }
        if (request.repetitionsPerSize() <= 0) {
            throw new BadRequestException("repetitionsPerSize must be positive");
        }
        if (request.seed() <= 0) {
            throw new BadRequestException("seed must be positive");
        }
    }

    /**
     * Validates that the provided problem instances in the request parameters are well-formed and compatible with their respective problem types.
     * @param problemIds the list of problem IDs specified in the request, used to determine which instances to validate
     * @param problemParams the map of problem parameters from the request, which should contain the instance data for each problem type as needed
     */
    private void validateProblemInstances(List<String> problemIds, Map<String, Object> problemParams) {
        Map<String, Object> params = problemParams != null ? problemParams : Map.of();

        if (problemIds.contains("tsp")) {
            Object tspInstance = params.get("tspInstance");
            if (tspInstance == null) {
                throw new BadRequestException("TSP problem requires a TSP instance");
            }

            try {
                InstanceMapper.toTspInstance(asInstanceMap(tspInstance, "tspInstance"));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid TSP instance: " + ex.getMessage());
            }
        }

        if (problemIds.contains("vrp")) {
            Object vrpInstance = params.get("vrpInstance");
            if (vrpInstance == null) {
                throw new BadRequestException("VRP problem requires a VRP instance");
            }

            try {
                InstanceMapper.toVrpInstance(asInstanceMap(vrpInstance, "vrpInstance"));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid VRP instance: " + ex.getMessage());
            }
        }
    }

    /**
     * Determines how often to log intermediate results during a run, based on the request.
     * If the request specifies a positive value for logEveryIterations,
     * that value is used. Otherwise, a default of 10 iterations is used for logging frequency.
     * @param request the run request containing the logEveryIterations setting
     * @return the number of iterations between logging intermediate results, either from the request or the default
     */
    public int resolveLogEveryIterations(RunRequest request) {
        return request.logEveryIterations() > 0 ? request.logEveryIterations() : 10;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asInstanceMap(Object value, String label) {
        if (!(value instanceof Map<?, ?> raw)) {
            throw new IllegalArgumentException(label + " must be a map");
        }
        return (Map<String, Object>) raw;
    }
}