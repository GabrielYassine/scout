package dk.dtu.scout.backend.controller;

import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.RuntimeStudyRequest;
import dk.dtu.scout.backend.service.RunOrchestratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoint for launching runs.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin
public class RunController {

    private final RunOrchestratorService runOrchestratorService;

    public RunController(RunOrchestratorService runOrchestratorService) {
        this.runOrchestratorService = runOrchestratorService;
    }

    @PostMapping("/run")
    public ResponseEntity<Void> run(@RequestBody RunRequest request) {
        runOrchestratorService.startRun(request);
        return ResponseEntity.accepted().build();
    }
    @PostMapping("/runtime-study")
    public ResponseEntity<Void> startStudy(@RequestBody RuntimeStudyRequest request) {
        runOrchestratorService.startRuntimeStudy(request);
        return ResponseEntity.accepted().build();
    }
}