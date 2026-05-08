package dk.dtu.scout.backend.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StatsControllerIntegrationTest {

    private static final String SERIES_WINDOW_PATH = "/api/stats/series-window";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    class SeriesWindowEndpoint {

        @Test
        void seriesWindow_computesStatsForPointsInsideRange() throws Exception {
            Map<String, Object> payload = basePayload(List.of(
                point(0.0, 100.0),
                point(1.0, 2.0),
                point(2.0, 4.0),
                point(3.0, 6.0),
                point(4.0, 100.0)
            ));

            postSeriesWindow(payload)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seriesName").value("fitness"))
                .andExpect(jsonPath("$.xAxisLabel").value("Evaluations"))
                .andExpect(jsonPath("$.yAxisLabel").value("Fitness"))
                .andExpect(jsonPath("$.xMin").value(1.0))
                .andExpect(jsonPath("$.xMax").value(3.0))
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.min").value(2.0))
                .andExpect(jsonPath("$.max").value(6.0))
                .andExpect(jsonPath("$.mean").value(4.0))
                .andExpect(jsonPath("$.stdDev").value(closeTo(1.63299, 0.0001)))
                .andExpect(jsonPath("$.median").value(4.0))
                .andExpect(jsonPath("$.q1").value(3.0))
                .andExpect(jsonPath("$.q3").value(5.0))
                .andExpect(jsonPath("$.iqr").value(2.0))
                .andExpect(jsonPath("$.slope").value(closeTo(2.0, 0.0001)))
                .andExpect(jsonPath("$.trend").value("up"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("dk.dtu.scout.backend.integrationtests.StatsControllerIntegrationTest#trendCases")
        void seriesWindow_reportsExpectedTrend(String label, List<List<Double>> points, String expectedTrend, double expectedSlope) throws Exception {
            postSeriesWindow(basePayload(points))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trend").value(expectedTrend))
                .andExpect(jsonPath("$.slope").value(closeTo(expectedSlope, 0.0001)));
        }

        @Test
        void seriesWindow_ignoresNullPointsAndPointsOutsideRange() throws Exception {
            List<Object> points = new ArrayList<>();
            points.add(point(0.0, 100.0));
            points.add(null);
            points.add(point(1.0, 2.0));
            points.add(point(2.0, 4.0));
            points.add(point(4.0, 100.0));

            postSeriesWindow(basePayload(points))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.min").value(2.0))
                .andExpect(jsonPath("$.max").value(4.0));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("dk.dtu.scout.backend.integrationtests.StatsControllerIntegrationTest#badRequestCases")
        void seriesWindow_rejectsInvalidRequests(String label, Map<String, Object> payload, String expectedMessage) throws Exception {
            assertBadRequest(payload, expectedMessage);
        }

        @Test
        void seriesWindow_rejectsMalformedJson() throws Exception {
            mockMvc.perform(post(SERIES_WINDOW_PATH).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed."))
                .andExpect(jsonPath("$.path").value(SERIES_WINDOW_PATH));
        }
    }

    private ResultActions postSeriesWindow(Object payload) throws Exception {
        return mockMvc.perform(post(SERIES_WINDOW_PATH).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload)));
    }

    private void assertBadRequest(Map<String, Object> payload, String expectedMessage) throws Exception {
        postSeriesWindow(payload)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value(expectedMessage))
            .andExpect(jsonPath("$.path").value(SERIES_WINDOW_PATH));
    }

    private static Stream<Arguments> trendCases() {
        return Stream.of(
            Arguments.of(
                "same x-values report flat trend",
                List.of(
                    point(2.0, 1.0),
                    point(2.0, 5.0),
                    point(2.0, 9.0)
                ),
                "flat",
                0.0
            ),
            Arguments.of(
                "negative slope reports down trend",
                List.of(
                    point(1.0, 6.0),
                    point(2.0, 4.0),
                    point(3.0, 2.0)
                ),
                "down",
                -2.0
            ),
            Arguments.of(
                "zero slope reports flat trend",
                List.of(
                    point(1.0, 5.0),
                    point(2.0, 5.0),
                    point(3.0, 5.0)
                ),
                "flat",
                0.0
            )
        );
    }

    private static Stream<Arguments> badRequestCases() {
        return Stream.of(
            Arguments.of(
                "empty points",
                basePayload(List.of()),
                "points must not be empty."
            ),
            Arguments.of(
                "missing points",
                payloadWithout("points"),
                "points must not be empty."
            ),
            Arguments.of(
                "invalid range",
                payloadWithRange(10.0, 1.0),
                "xMin must be less than or equal to xMax."
            ),
            Arguments.of(
                "range contains no points",
                payloadWithRangeAndPoints(
                    10.0,
                    20.0,
                    List.of(point(1.0, 1.0), point(2.0, 2.0))
                ),
                "No points fall inside the requested x-range."
            )
        );
    }

    private static Map<String, Object> basePayload(List<?> points) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("seriesName", "fitness");
        payload.put("xAxisLabel", "Evaluations");
        payload.put("yAxisLabel", "Fitness");
        payload.put("xMin", 1.0);
        payload.put("xMax", 3.0);
        payload.put("points", points);
        return payload;
    }

    private static Map<String, Object> payloadWithout(String key) {
        Map<String, Object> payload = basePayload(List.of(point(1.0, 1.0)));
        payload.remove(key);
        return payload;
    }

    private static Map<String, Object> payloadWithRange(double xMin, double xMax) {
        return payloadWithRangeAndPoints(xMin, xMax, List.of(point(1.0, 1.0)));
    }

    private static Map<String, Object> payloadWithRangeAndPoints(double xMin, double xMax, List<List<Double>> points) {
        Map<String, Object> payload = basePayload(points);
        payload.put("xMin", xMin);
        payload.put("xMax", xMax);
        return payload;
    }

    private static List<Double> point(double x, double y) {
        return List.of(x, y);
    }
}