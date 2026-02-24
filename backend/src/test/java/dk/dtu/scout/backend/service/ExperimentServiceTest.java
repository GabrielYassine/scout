package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.run.BatchRunResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExperimentServiceTest {

    private ExperimentService experimentService;

    @BeforeEach
    void setUp() {
        experimentService = new ExperimentService();
    }

    @Test
    @DisplayName("(1+1) EA on OneMax should converge within expected iterations")
    void testOneMaxWith1Plus1EA() {
        RunRequest request = new RunRequest(
                List.of("bitstring"),
                Map.of("n", 100),
                List.of("onemax"),
                null,
                List.of("single-bit-flip"),  // Standard bit-flip mutation for (1+1) EA
                null,
                List.of("default"),
                Map.of("lambda", 1),  // (1+1) EA: 1 parent, 1 offspring
                List.of("elitist"),  // (1+1) EA: accept only improvements
                null,
                List.of("fitness"),
                List.of("max-iterations"),
                Map.of("maxIterations", 50000),
                12345L,
                1
        );

        BatchRunResponse response = experimentService.run(request);
        assertNotNull(response);
    }


    @Test
    @DisplayName("(1+1) EA on LeadingOnes should converge within expected iterations")
    void testLeadingOnesWith1Plus1EA() {
        RunRequest request = new RunRequest(
                List.of("bitstring"),
                Map.of("n", 50),
                List.of("leadingones"),
                null,
                List.of("single-bit-flip"),  // Standard bit-flip mutation for (1+1) EA
                null,
                List.of("default"),
                Map.of("lambda", 1),  // (1+1) EA: 1 parent, 1 offspring
                List.of("elitist"),  // (1+1) EA: accept only improvements
                null,
                List.of("fitness"),
                List.of("max-iterations"),
                Map.of("maxIterations", 200000),
                67890L,
                1
        );

        BatchRunResponse response = experimentService.run(request);
        assertNotNull(response);
    }
}