package dk.dtu.scout.backend.service;

import java.util.List;

import dk.dtu.scout.backend.dto.AlgoDef;
import dk.dtu.scout.backend.dto.ParamDef;
import dk.dtu.scout.backend.dto.ProblemDef;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {

    public List<ProblemDef> problems() {
        return List.of(new ProblemDef(
                "onemax",
                "OneMax",
                List.of(new ParamDef(
                        "n",
                        "Bitstring length (n)",
                        "int",
                        100,
                        1.0,
                        null),
                        new ParamDef(
                                "seed",
                                "Seed",
                                "long",
                                32L,
                                null,
                                null))));
    }



    public List<AlgoDef> algorithms() {
        return List.of(new AlgoDef(
                "1p1-ea",
                "(1+1) EA",
                List.of(new ParamDef(
                        "maxIterations",
                        "Max iterations",
                        "int",
                        10.000,
                        1.0, null),
                        new ParamDef(
                                "seed",
                                "Seed",
                                "long",
                                32L,
                                null,
                                null))));
    }
}
