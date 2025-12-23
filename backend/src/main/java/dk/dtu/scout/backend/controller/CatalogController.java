package dk.dtu.scout.backend.controller;

import dk.dtu.scout.backend.dto.CatalogResponse;
import dk.dtu.scout.backend.service.CatalogService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/catalog")
    public CatalogResponse catalog() {
        return new CatalogResponse(catalog.problems(), catalog.algorithms());
    }
}

