package dk.dtu.scout.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dtu.scout.backend.dto.template.ExperimentTemplateDto;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class TemplateService {

    private final ObjectMapper mapper = new ObjectMapper();

    public List<ExperimentTemplateDto> listTemplates() {
        try {
            var resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:templates/*.json");

            List<ExperimentTemplateDto> out = new ArrayList<>();
            for (Resource r : resources) {
                try (InputStream in = r.getInputStream()) {
                    ExperimentTemplateDto t = mapper.readValue(in, new TypeReference<>() {});
                    out.add(t);
                }
            }
            return out;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load templates", e);
        }
    }
}