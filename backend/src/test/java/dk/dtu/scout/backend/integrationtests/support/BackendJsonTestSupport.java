package dk.dtu.scout.backend.integrationtests.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public final class BackendJsonTestSupport {

    private BackendJsonTestSupport() {
    }

    public static ResultActions postJson(MockMvc mockMvc, ObjectMapper objectMapper, String url, Object payload) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload)));
    }

    public static Map<String, Object> readJsonObject(ObjectMapper objectMapper, MvcResult result) throws Exception {
        return objectMapper.readValue(
            result.getResponse().getContentAsString(),
            new TypeReference<>() {}
        );
    }

    public static String stringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> mapValue(Map<String, Object> map, String key) {
        Object value = map.get(key);

        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("Expected key '" + key + "' to contain a map");
        }

        return (Map<String, Object>) value;
    }
}