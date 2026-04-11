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
 * REST endpoint for instance utilities.
 */
@RestController
@RequestMapping("/api/instance")
@CrossOrigin
public class InstanceController {

    private final InstanceService instanceService;

    public InstanceController(InstanceService instanceService) {
        this.instanceService = instanceService;
    }

    @PostMapping("/export")
    public ResponseEntity<String> exportInstance(@RequestBody Map<String, Object> payload) {
        try {
            String exportType = payload == null ? null : String.valueOf(payload.get("exportType"));
            String content = instanceService.exportInstance(exportType, payload);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/plain; charset=utf-8")
                .body(content);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }
}
