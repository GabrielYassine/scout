package dk.dtu.scout.backend.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StatsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    class SeriesWindowEndpoint {

        @Test
        void seriesWindow_withSameXValuesReportsFlatTrend() throws Exception {
            Map<String, Object> payload = basePayload(List.of(
                point(2.0, 1.0),
                point(2.0, 5.0),
                point(2.0, 9.0)
            ));

            postSeriesWindow(payload)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.mean").value(5.0))
                .andExpect(jsonPath("$.slope").value(0.0))
                .andExpect(jsonPath("$.trend").value("flat"));
        }

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

        @Test
        void seriesWindow_reportsDownTrend() throws Exception {
            Map<String, Object> payload = basePayload(List.of(
                point(1.0, 6.0),
                point(2.0, 4.0),
                point(3.0, 2.0)
            ));

            postSeriesWindow(payload)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trend").value("down"))
                .andExpect(jsonPath("$.slope").value(closeTo(-2.0, 0.0001)));
        }

        @Test
        void seriesWindow_reportsFlatTrendWhenSlopeIsSmall() throws Exception {
            Map<String, Object> payload = basePayload(List.of(
                point(1.0, 5.0),
                point(2.0, 5.0),
                point(3.0, 5.0)
            ));

            postSeriesWindow(payload)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trend").value("flat"))
                .andExpect(jsonPath("$.slope").value(0.0));
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

        @Test
        void seriesWindow_rejectsEmptyPoints() throws Exception {
            assertBadRequest(basePayload(List.of()), "points must not be empty.");
        }

        @Test
        void seriesWindow_rejectsMissingPoints() throws Exception {
            Map<String, Object> payload = basePayload(List.of(point(1.0, 1.0)));
            payload.remove("points");

            assertBadRequest(payload, "points must not be empty.");
        }

        @Test
        void seriesWindow_rejectsInvalidRange() throws Exception {
            Map<String, Object> payload = basePayload(List.of(point(1.0, 1.0)));
            payload.put("xMin", 10.0);
            payload.put("xMax", 1.0);

            assertBadRequest(payload, "xMin must be less than or equal to xMax.");
        }

        @Test
        void seriesWindow_rejectsRangeWithNoPointsInside() throws Exception {
            Map<String, Object> payload = basePayload(List.of(
                point(1.0, 1.0),
                point(2.0, 2.0)
            ));
            payload.put("xMin", 10.0);
            payload.put("xMax", 20.0);

            assertBadRequest(payload, "No points fall inside the requested x-range.");
        }

        @Test
        void seriesWindow_rejectsMalformedJson() throws Exception {
            mockMvc.perform(post("/api/stats/series-window").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed."))
                .andExpect(jsonPath("$.path").value("/api/stats/series-window"));
        }
    }

    private ResultActions postSeriesWindow(Object payload) throws Exception {
        return mockMvc.perform(post("/api/stats/series-window")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)));
    }

    private void assertBadRequest(Map<String, Object> payload, String expectedMessage) throws Exception {
        postSeriesWindow(payload)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value(expectedMessage))
            .andExpect(jsonPath("$.path").value("/api/stats/series-window"));
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

    private static List<Double> point(double x, double y) {
        return List.of(x, y);
    }
}