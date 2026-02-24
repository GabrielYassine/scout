package dk.dtu.scout.backend.controller;

import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.run.BatchRunResponse;
import dk.dtu.scout.backend.service.ExperimentService;
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
    public BatchRunResponse run(@RequestBody RunRequest request) {
        System.out.println("Received run request: " + request);
        return experimentService.run(request);
    }
}
