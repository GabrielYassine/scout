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
     *
     * Prepares a run by creating a sessionId and runId.
     * If a sessionId is provided, it will be reused if it exists, and a new runId will be created for the new run.
     * If the sessionId does not exist, a new sessionId will be created along with a runId.
     * This allows the system to cancel previous runs associated with the same sessionId.
     * @param request a DTO containing an optional sessionId.
     * @return a DTO containing the sessionId to be used for starting the run.
     */
    @PostMapping("/run/prepare")
    public ResponseEntity<PrepareRunResponse> prepareRun(@RequestBody(required = false) PrepareRunRequest request) {
        String requestedSessionId = request != null ? request.sessionId() : null;
        return ResponseEntity.ok(runOrchestratorService.prepareRun(requestedSessionId));
    }

    /**
     *
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