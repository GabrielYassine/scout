package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.permutation.TSPDto;
import dk.dtu.scout.backend.exception.BadRequestException;
import dk.dtu.scout.backend.util.InstanceMapper;
import dk.dtu.scout.backend.util.ViewMapper;
import dk.dtu.scout.datatypes.TSPInstance;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class TSPInstanceService {

    public TSPDto uploadInstance(String content) throws IOException {
        try {
            TSPInstance instance = InstanceMapper.parseTsplib(content);
            return ViewMapper.toTspDto(instance);
        } catch (IOException e) {
            throw new BadRequestException("Invalid TSP file");
        }
    }
}
