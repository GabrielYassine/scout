package dk.dtu.scout.backend.integrationtests.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dtu.scout.backend.dto.request.PrepareRunResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static dk.dtu.scout.backend.integrationtests.support.BackendJsonTestSupport.postJson;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class RunPrepareTestSupport {

    private RunPrepareTestSupport() {
    }

    public static ResultActions postRunPrepare(MockMvc mockMvc, ObjectMapper objectMapper, Object payload) throws Exception {
        return postJson(mockMvc, objectMapper, "/api/run/prepare", payload);
    }

    public static ResultActions postRuntimeStudyPrepare(MockMvc mockMvc, ObjectMapper objectMapper, Object payload) throws Exception {
        return postRunPrepare(mockMvc, objectMapper, payload);
    }

    public static PrepareRunResponse prepareRun(MockMvc mockMvc, ObjectMapper objectMapper, Map<String, Object> payload) throws Exception {
        return prepare(mockMvc, objectMapper, payload);
    }

    public static PrepareRunResponse prepareRuntimeStudy(MockMvc mockMvc, ObjectMapper objectMapper, Map<String, Object> payload) throws Exception {
        return prepare(mockMvc, objectMapper, payload);
    }

    private static PrepareRunResponse prepare(MockMvc mockMvc, ObjectMapper objectMapper, Map<String, Object> payload) throws Exception {
        MvcResult result = postRunPrepare(mockMvc, objectMapper, payload)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").isString())
            .andExpect(jsonPath("$.executionId").isString())
            .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), PrepareRunResponse.class);
    }
}