package dk.dtu.scout.backend.controller;

import dk.dtu.scout.backend.dto.catalog.ComponentDef;
import dk.dtu.scout.backend.dto.catalog.ParamDef;
import dk.dtu.scout.backend.service.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CatalogController.class)
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogService catalogService;

    @Test
    void catalog_returnsCatalogResponse() throws Exception {
        ComponentDef searchSpace = new ComponentDef(
            "searchSpace",
            "bitstring",
            "Bitstring",
            "Bitstring search space",
            List.of(new ParamDef("n", "N", "int", 10, 1.0, null, null)),
            List.of("bitstring")
        );

        when(catalogService.searchSpaces()).thenReturn(List.of(searchSpace));
        when(catalogService.problems()).thenReturn(List.of());
        when(catalogService.generators()).thenReturn(List.of());
        when(catalogService.selectionRules()).thenReturn(List.of());
        when(catalogService.populationModels()).thenReturn(List.of());
        when(catalogService.parentSelectionRules()).thenReturn(List.of());
        when(catalogService.crossovers()).thenReturn(List.of());
        when(catalogService.stopConditions()).thenReturn(List.of());
        when(catalogService.observers()).thenReturn(List.of());

        mockMvc.perform(get("/api/catalog"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.searchSpaces[0].id").value("bitstring"))
            .andExpect(jsonPath("$.searchSpaces[0].params[0].key").value("n"))
            .andExpect(jsonPath("$.problems").isArray());
    }
}
