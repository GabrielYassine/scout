package dk.dtu.scout.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DistanceUtilsTest {

    @Test
    void euclideanRounded_returnsRoundedEuclideanDistance() {
        assertEquals(5.0, DistanceUtils.euclideanRounded(
            new double[] {0.0, 0.0},
            new double[] {3.0, 4.0}
        ));

        assertEquals(1.0, DistanceUtils.euclideanRounded(
            new double[] {0.0, 0.0},
            new double[] {0.6, 0.0}
        ));
    }
}