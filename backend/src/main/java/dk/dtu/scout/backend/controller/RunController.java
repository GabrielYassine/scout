package dk.dtu.scout.backend.controller;

import dk.dtu.scout.backend.dto.request.PrepareRunRequest;
import dk.dtu.scout.backend.dto.request.PrepareRunResponse;
import dk.dtu.scout.backend.dto.request.RunRequest;
import dk.dtu.scout.backend.dto.request.RuntimeStudyRequest;
import dk.dtu.scout.backend.service.RunOrchestratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoint for preparing and starting runs, and starting runtime studies.
 * @author s235257 & Ahmed
 */
@RestController
@RequestMapping("/api")
@CrossOrigin
public class RunController {

    private final RunOrchestratorService runOrchestratorService;

    public RunController(RunOrchestratorService runOrchestratorService) {
        this.runOrchestratorService = runOrchestratorService;
    }

    /**
     * Prepares an execution by generating a sessionId and executionId, injecting
     * them into the provided draft request, and validating the final request.
     *
     * This lets validation errors appear on the LabPage before navigation to RunPage.
     *
     * @param request the draft execution request to validate
     * @return a DTO containing the sessionId and executionId to use for websocket execution
     */
    @PostMapping("/run/prepare")
    public ResponseEntity<PrepareRunResponse> prepareRun(@RequestBody PrepareRunRequest request) {
        return ResponseEntity.ok(runOrchestratorService.prepareRun(request));
    }

    /**
     * Starts an async run, the run will be associated with the sessionId provided in the request.
     * @param request a DTO containing the sessionId and all parameters for the run.
     * @return an empty response with status 202 Accepted, indicating that the run has been accepted for processing.
     */
    @PostMapping("/run")
    public ResponseEntity<Void> run(@RequestBody RunRequest request) {
        runOrchestratorService.startRun(request);
        return ResponseEntity.accepted().build();
    }

    /**
     * Starts an async runtime study, the study will be associated with the sessionId provided in the request.
     * @param request a DTO containing the sessionId and all parameters for the runtime study.
     * @return an empty response with status 202 Accepted, indicating that the runtime study has been accepted for processing.
     */
    @PostMapping("/runtime-study")
    public ResponseEntity<Void> startStudy(@RequestBody RuntimeStudyRequest request) {
        runOrchestratorService.startRuntimeStudy(request);
        return ResponseEntity.accepted().build();
    }
}