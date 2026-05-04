package dk.dtu.scout.backend.controller;

import dk.dtu.scout.backend.service.InstanceService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for handling instance import and export.
 * Delegates to InstanceService for parsing, validation, normalization, and formatting of TSP and VRP instances.
 * @author s235257
 */
@RestController
@RequestMapping("/api/instance")
@CrossOrigin
public class InstanceController {

    private final InstanceService instanceService;

    public InstanceController(InstanceService instanceService) {
        this.instanceService = instanceService;
    }

    @PostMapping("/import")
    public ResponseEntity<?> importInstance(@RequestBody Map<String, Object> payload) {
        try {
            String content = String.valueOf(payload.getOrDefault("content", ""));

            Map<String, Object> response = instanceService.importInstance(content);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    @PostMapping("/export")
    public ResponseEntity<String> exportInstance(@RequestBody Map<String, Object> payload) {
        try {
            String content = instanceService.exportInstance(payload);

            // this tells FE to treat it as plain text file and not try to parse it as JSON, and also ensures correct UTF-8 encoding
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "text/plain; charset=utf-8").body(content);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }
}
