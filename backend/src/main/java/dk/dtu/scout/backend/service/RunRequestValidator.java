package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.RuntimeStudyRequest;
import dk.dtu.scout.backend.exception.BadRequestException;
import dk.dtu.scout.backend.instance.InstanceMapper;
import dk.dtu.scout.backend.instance.InstanceValidator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Validates run requests, that includes global settings, components, their parameters, and problem instances.
 * @author s235257 @ Ahmed
 */
@Service
public class RunRequestValidator {

    public void validateRunRequest(RunRequest request) {
        if (request == null) {
            throw new BadRequestException("Request must be provided");
        }
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
        if (request.runId() == null || request.runId().isBlank()) {
            throw new BadRequestException("runId must be specified");
        }
        if (request.runTimes() <= 0) {
            throw new BadRequestException("runTimes must be positive");
        }
        if (request.logEveryIterations() < 0) {
            throw new BadRequestException("logEveryIterations must be zero or positive");
        }

        validateProblemInstances(request.problemIds(), request.problemParams());
    }

    public void validateRuntimeStudyRequest(RuntimeStudyRequest request) {
        if (request == null) {
            throw new BadRequestException("Request must be provided");
        }
        if (request.studyId() == null || request.studyId().isBlank()) {
            throw new BadRequestException("studyId must be specified");
        }
        if (request.searchSpaceId() == null || request.searchSpaceId().isBlank()) {
            throw new BadRequestException("Search space must be specified");
        }
        if (request.problemId() == null || request.problemId().isBlank()) {
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

        validateRuntimeStudySemantics(request);
    }

    private void validateRuntimeStudySemantics(RuntimeStudyRequest request) {
        if ("tsp".equals(request.problemId()) || "vrp".equals(request.problemId())) {
            throw new BadRequestException(
                    "Runtime study currently supports theoretical size-based problems only"
            );
        }
    }

    private void validateProblemInstances(List<String> problemIds, Map<String, Object> problemParams) {
        Map<String, Object> params = problemParams != null ? problemParams : Map.of();

        if (problemIds.contains("tsp")) {
            Object tspInstance = params.get("tspInstance");
            if (tspInstance == null) {
                throw new BadRequestException("TSP problem requires a TSP instance");
            }

            try {
                InstanceValidator.validateTsp(InstanceMapper.toTspInstance(asInstanceMap(tspInstance, "tspInstance")));
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
                InstanceValidator.validateVrp(InstanceMapper.toVrpInstance(asInstanceMap(vrpInstance, "vrpInstance")));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid VRP instance: " + ex.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asInstanceMap(Object value, String label) {
        if (!(value instanceof Map<?, ?> raw)) {
            throw new IllegalArgumentException(label + " must be a map");
        }
        return (Map<String, Object>) raw;
    }

    public int resolveLogEveryIterations(RunRequest request) {
        return request.logEveryIterations() > 0 ? request.logEveryIterations() : 10;
    }
}