package dk.dtu.scout.backend.controller;

import dk.dtu.scout.backend.dto.request.PrepareRunRequest;
import dk.dtu.scout.backend.dto.request.PrepareRunResponse;
import dk.dtu.scout.backend.service.RunOrchestratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoint for preparing runs and runtime studies.
 * User sends a request and gets back a sessionId and executionId.
 * @author s235257 & s230632
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
     * This lets validation errors appear on the LabPage before navigation to RunPage.
     * @param request the draft execution request to validate
     * @return a DTO containing the sessionId and executionId to use for websocket execution
     */
    @PostMapping("/run/prepare")
    public ResponseEntity<PrepareRunResponse> prepareRun(@RequestBody PrepareRunRequest request) {
        return ResponseEntity.ok(runOrchestratorService.prepareRun(request));
    }
}