package dk.dtu.scout.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dtu.scout.backend.dto.template.ExperimentTemplateDto;
import dk.dtu.scout.backend.exception.TemplateLoadException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
                    out.add(mapper.readValue(in, ExperimentTemplateDto.class));
                }
            }
            return out;
        } catch (IOException e) {
            throw new TemplateLoadException("Failed to load templates", e);
        }
    }
}