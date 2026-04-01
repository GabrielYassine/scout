package dk.dtu.scout.backend.controller;

import dk.dtu.scout.backend.dto.permutation.TSPDto;
import dk.dtu.scout.backend.service.TSPInstanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * REST endpoint for TSP instance utilities.
 */
@RestController
@RequestMapping("/api/tsp")
@CrossOrigin
public class TSPController {

    private final TSPInstanceService tspInstanceService;

    public TSPController(TSPInstanceService tspInstanceService) {
        this.tspInstanceService = tspInstanceService;
    }

    // We take the raw content of the uploaded TSP file as a string in the request body, and parse it in the service layer
    // We do this instead of a DTO because we expect the user to upload a file in a specific format
    @PostMapping("/upload")
    public ResponseEntity<TSPDto> uploadTSPInstance(@RequestBody String content) {
        try {
            TSPDto tspDto = tspInstanceService.uploadInstance(content);
            return ResponseEntity.ok(tspDto);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
