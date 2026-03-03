package dk.dtu.scout.backend.dto.permutation;

import java.util.List;

public record TSPDto(
    String name,
    int dimension,
    List<CityDto> cities,
    int[] tour,
    double tourLength
) {}
