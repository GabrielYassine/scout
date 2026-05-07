package dk.dtu.scout.backend.integrationtests;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the CatalogController,
 * verifying that the /api/catalog endpoint returns the expected components registered in the Spring context.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CatalogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void catalog_returnsRealRegisteredBackendComponents() throws Exception {
        mockMvc.perform(get("/api/catalog"))
            .andExpect(status().isOk())

            .andExpect(jsonPath("$.searchSpaces.length()", greaterThan(0)))
            .andExpect(jsonPath("$.problems.length()", greaterThan(0)))
            .andExpect(jsonPath("$.generators.length()", greaterThan(0)))
            .andExpect(jsonPath("$.selectionRules.length()", greaterThan(0)))
            .andExpect(jsonPath("$.populationModels.length()", greaterThan(0)))
            .andExpect(jsonPath("$.parentSelectionRules.length()", greaterThan(0)))
            .andExpect(jsonPath("$.stopConditions.length()", greaterThan(0)))
            .andExpect(jsonPath("$.observers.length()", greaterThan(0)))

            .andExpect(jsonPath("$.searchSpaces[*].id", hasItem("bitstring")))
            .andExpect(jsonPath("$.problems[*].id", hasItem("onemax")))
            .andExpect(jsonPath("$.generators[*].id", hasItem("bit-flip")))
            .andExpect(jsonPath("$.selectionRules[*].id", hasItem("mu-plus-lambda")))
            .andExpect(jsonPath("$.populationModels[*].id", hasItem("mu-lambda")))
            .andExpect(jsonPath("$.parentSelectionRules[*].id", hasItem("elitist-parents")))
            .andExpect(jsonPath("$.stopConditions[*].id", hasItem("max-evaluations")))
            .andExpect(jsonPath("$.observers[*].id", hasItem("fitness")));
    }

    @Test
    void catalog_mapsComponentMetadataAndParameters() throws Exception {
        mockMvc.perform(get("/api/catalog"))
            .andExpect(status().isOk())

            .andExpect(jsonPath("$.searchSpaces[0].kind").isString())
            .andExpect(jsonPath("$.searchSpaces[0].id").isString())
            .andExpect(jsonPath("$.searchSpaces[0].displayName").isString())
            .andExpect(jsonPath("$.searchSpaces[0].description").isString())
            .andExpect(jsonPath("$.searchSpaces[0].params").isArray())
            .andExpect(jsonPath("$.searchSpaces[0].supportedSearchSpaces").isArray());
    }
}