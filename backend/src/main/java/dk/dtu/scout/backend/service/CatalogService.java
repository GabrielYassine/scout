package dk.dtu.scout.backend.service;

import java.util.List;

import dk.dtu.scout.backend.dto.catalog.*;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {

    /** Returns the list of available search spaces.
     * @return List of SearchSpaceDef
     */
    public List<SearchSpaceDef> searchSpaces() {
        return List.of(
            new SearchSpaceDef(
                "bitstring",
                "BitString",
                "Binary string representation for boolean optimization problems",
                List.of(
                    new ParamDef(
                        "n",
                        "Length (n)",
                        "int",
                        100,
                        1.0,
                        null
                    )
                )
            )
        );
    }

    /** Returns the list of available problems.
     * @return List of ProblemDef
     */
    public List<ProblemDef> problems() {
        return List.of(

            new ProblemDef(
                "onemax",
                "OneMax",
                "Maximize the number of ones in a bitstring",
                List.of(
                    new ParamDef(
                        "n",
                        "Bitstring length (n)",
                        "int",
                        100,
                        1.0,
                        null
                    )
                )
            ),

            new ProblemDef(
                "leadingones",
                "LeadingOnes",
                "Maximize the number of leading ones in a bitstring",
                List.of(
                    new ParamDef(
                        "n",
                        "Bitstring length (n)",
                        "int",
                        100,
                        1.0,
                        null
                    )
                )
            )
        );
    }

    /** Returns the list of available algorithms.
     * @return List of AlgoDef
     */
    public List<AlgoDef> algorithms() {
        return List.of(

            new AlgoDef(
                "1p1-ea",
                "(1+1) EA",
                "A simple evolutionary algorithm",
                List.of()
            ),

            new AlgoDef(
                "sa",
                "Simulated Annealing",
                "An optimization algorithm inspired by the annealing process in metallurgy",
                List.of()
            )
        );
    }

    /**
     * Returns the list of available mutations.
     * @return List of MutationDef
     */
    public List<MutationDef> mutations() {
        return List.of(
            new MutationDef(
                "bit-flip",
                "Bit Flip Mutation",
                "Flips each bit in a bitstring with a certain probability",
                List.of(
                    new ParamDef(
                        "flipProbability",
                        "Flip Probability",
                            "string",
                            "1/n",
                        null,
                        null
                    )
                )
            ),
            new MutationDef(
                "single-bit-flip",
                "Single Bit Flip Mutation",
                "Flips a single randomly chosen bit in a bitstring",
                List.of()
            )
        );
    }

    /**
     * Returns the list of available acceptance rules.
     * @return List of AcceptanceRuleDef
     */
    public List<AcceptanceRuleDef> acceptanceRules() {
        return List.of(
            new AcceptanceRuleDef(
                "elitist",
                "Elitist Acceptance",
                "Accepts only solutions that are better than or equal to the current solution",
                List.of()
            ),
            new AcceptanceRuleDef(
                "simulated-annealing",
                "Simulated Annealing Acceptance",
                "Accepts worse solutions with a probability that decreases over time",
                List.of(
                        new ParamDef("initialTemperature", "Initial temperature (T0)", "double", 5.0, null, null),
                        new ParamDef("coolingRate", "Cooling rate", "double", 0.995, null, null),
                        new ParamDef("minTemperature", "Min temperature", "double", 1e-6, null, null)
                )
            )
        );
    }

    /**
     * Returns the list of available stop conditions.
     * @return List of StopConditionDef
     */
    public List<StopConditionDef> stopConditions() {
        return List.of(
            new StopConditionDef(
                "max-iterations",
                "Max Iterations",
                "Stops the algorithm after a maximum number of iterations",
                List.of(
                    new ParamDef(
                        "maxIterations",
                        "Max iterations",
                        "int",
                        10_000,
                        1.0,
                        null
                    )
                )
            )
        );
    }

    /**
     * Returns the list of available observers.
     * @return List of ObserverDef
     */
    public List<ObserverDef> observers() {
        return List.of(
            new ObserverDef(
                "fitness",
                "Fitness Tracker",
                "Tracks fitness values over time",
                List.of()
            ),
            new ObserverDef(
                "acceptance-rate",
                "Acceptance Rate",
                "Tracks the acceptance rate of solutions",
                List.of()
            ),
            new ObserverDef(
                "improvements",
                "Improvement Tracker",
                "Tracks improvements in the best solution",
                List.of()
            )
        );
    }
}