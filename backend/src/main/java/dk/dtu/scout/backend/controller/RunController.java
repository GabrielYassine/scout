package dk.dtu.scout.backend.controller;

import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.service.ExperimentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class RunController {

    private final ExperimentService experimentService;

    public RunController(ExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    @PostMapping("/run")
    public ResponseEntity<Void> run(@RequestBody RunRequest request) {
        experimentService.startRun(request);
        return ResponseEntity.accepted().build();
    }
}