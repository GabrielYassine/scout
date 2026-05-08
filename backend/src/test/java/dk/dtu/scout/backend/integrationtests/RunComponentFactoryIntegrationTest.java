package dk.dtu.scout.backend.integrationtests;

import dk.dtu.scout.backend.service.RunComponentFactory;
import dk.dtu.scout.problems.TSP;
import dk.dtu.scout.problems.VRP;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static dk.dtu.scout.backend.integrationtests.support.InstanceFixtures.smallTspInstancePayload;
import static dk.dtu.scout.backend.integrationtests.support.InstanceFixtures.smallVrpInstancePayload;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RunComponentFactoryIntegrationTest {

    @Autowired
    private RunComponentFactory runComponentFactory;

    @Nested
    class ProblemSpecificParamMapping {

        @Test
        void createProblem_mapsTspInstanceParams() {
            TSP tspProblem = assertInstanceOf(
                TSP.class,
                runComponentFactory.createProblem("tsp", 2, Map.of("tspInstance", smallTspInstancePayload()))
            );

            assertNotNull(tspProblem.getInstance());
            assertEquals("tiny", tspProblem.getInstance().getName());
            assertEquals(2, tspProblem.getInstance().getDimension());
        }

        @Test
        void createProblem_mapsVrpInstanceParams() {
            VRP vrpProblem = assertInstanceOf(
                VRP.class,
                runComponentFactory.createProblem("vrp", 0, Map.of("vrpInstance", smallVrpInstancePayload()))
            );

            assertNotNull(vrpProblem.getInstance());
            assertEquals("tiny", vrpProblem.getInstance().getName());
            assertEquals(1, vrpProblem.getInstance().getCustomerCount());
        }
    }
}