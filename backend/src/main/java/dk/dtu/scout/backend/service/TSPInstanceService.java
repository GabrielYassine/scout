package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.util.TSPLibParser;
import dk.dtu.scout.problems.TSPInstance;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class TSPInstanceService {

    public void uploadInstance(String content) throws IOException {
        TSPInstance instance = TSPLibParser.parse(content);
        // return instance to frontend for display
    }
}
