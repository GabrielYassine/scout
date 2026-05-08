package dk.dtu.scout.backend.integrationtests.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static dk.dtu.scout.backend.integrationtests.support.BackendJsonTestSupport.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public final class RunPrepareTestSupport {

    private RunPrepareTestSupport() {
    }

    public static ResultActions postRunPrepare(
        MockMvc mockMvc,
        ObjectMapper objectMapper,
        Object payload
    ) throws Exception {
        return postJson(mockMvc, objectMapper, "/api/run/prepare", payload);
    }

    public static ResultActions postRuntimeStudyPrepare(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            Object payload
    ) throws Exception {
        return postJson(mockMvc, objectMapper, "/api/run/prepare", payload);
    }

    public static PreparedExecution prepareRun(
        MockMvc mockMvc,
        ObjectMapper objectMapper,
        Map<String, Object> payload
    ) throws Exception {
        MvcResult result = postRunPrepare(mockMvc, objectMapper, payload)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").isString())
            .andExpect(jsonPath("$.executionId").isString())
            .andReturn();

        Map<String, Object> response = readJsonObject(objectMapper, result);

        return new PreparedExecution(
            stringValue(response, "sessionId"),
            stringValue(response, "executionId")
        );
    }

    public static PreparedExecution prepareRuntimeStudy(
        MockMvc mockMvc,
        ObjectMapper objectMapper,
        Map<String, Object> payload
    ) throws Exception {
        MvcResult result = postRuntimeStudyPrepare(mockMvc, objectMapper, payload)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").isString())
            .andExpect(jsonPath("$.executionId").isString())
            .andReturn();

        Map<String, Object> response = readJsonObject(objectMapper, result);

        return new PreparedExecution(
            stringValue(response, "sessionId"),
            stringValue(response, "executionId")
        );
    }
}