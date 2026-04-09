package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.RuntimeStudyRequest;
import dk.dtu.scout.backend.exception.BadRequestException;
import org.springframework.stereotype.Service;

@Service
public class RuntimeStudyRequestValidator {

    public void validate(RuntimeStudyRequest request) {
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
        if (request.stopConditionIds() == null || request.stopConditionIds().isEmpty()|| !request.stopConditionIds().contains("optimum-reached")) {
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



}