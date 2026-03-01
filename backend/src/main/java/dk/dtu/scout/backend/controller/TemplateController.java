package dk.dtu.scout.backend.controller;

import dk.dtu.scout.backend.dto.template.ExperimentTemplateDto;
import dk.dtu.scout.backend.service.TemplateService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class TemplateController {

    private final TemplateService templates;

    public TemplateController(TemplateService templates) {
        this.templates = templates;
    }

    @GetMapping("/templates")
    public List<ExperimentTemplateDto> templates() {
        return templates.listTemplates();
    }
}