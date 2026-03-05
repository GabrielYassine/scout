package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.permutation.CityDto;
import dk.dtu.scout.backend.dto.permutation.TSPDto;
import dk.dtu.scout.backend.util.TSPLibParser;
import dk.dtu.scout.problems.TSPInstance;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class TSPInstanceService {

    public TSPDto uploadInstance(String content) throws IOException {
        TSPInstance instance = TSPLibParser.parse(content);

        // TODO: Following code should be moved into a viewmapper class.

        double[][] coordinates = instance.getCoordinates();
        List<CityDto> cities = new ArrayList<>();

        for (int i = 0; i < coordinates.length; i++) {
            cities.add(new CityDto(i, coordinates[i][0], coordinates[i][1]));
        }

        return new TSPDto(
            instance.getName(),
            instance.getDimension(),
            cities,
            null, // We will just show cities without a route when they upload.
            0.0 // no route, so distance is 0.0
        );
    }
}
