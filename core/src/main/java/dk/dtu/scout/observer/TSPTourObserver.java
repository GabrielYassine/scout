package dk.dtu.scout.observer;

import dk.dtu.scout.ConfigurationContext;
import dk.dtu.scout.Parameter;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.problems.TSPInstance;
import dk.dtu.scout.problems.TSPProblem;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class TSPTourObserver implements Observer<int[]> {

    private final List<int[]> tourHistory = new ArrayList<>();
    private List<Map<String, Double>> cities;
    private boolean citiesLogged = false;

    @Override
    public String id() {
        return "tsp-tour";
    }

    @Override
    public String displayName() {
        return "TSP Tour Observer";
    }

    @Override
    public String description() {
        return "Tracks the tour for TSP visualization";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    public void setCities(List<Map<String, Double>> cities) {
        this.cities = cities;
    }

    public void configure(Map<String, Object> params, ConfigurationContext context) {
        if (context.hasProblem()) {
            Problem<?> problem = context.getProblem();
            if (problem instanceof TSPProblem) {
                TSPProblem tspProblem = (TSPProblem) problem;
                TSPInstance instance = tspProblem.getInstance();
                if (instance != null) {
                    double[][] coords = instance.getCoordinates();
                    List<Map<String, Double>> cityList = new ArrayList<>();
                    for (double[] coord : coords) {
                        Map<String, Double> city = new HashMap<>();
                        city.put("x", coord[0]);
                        city.put("y", coord[1]);
                        cityList.add(city);
                    }
                    this.cities = cityList;
                }
            }
        }
        if (params != null && !params.isEmpty()) {
            configure(params);
        }
    }

    @Override
    public void onStep(RunState<int[]> state, RunLog log) {
        if (!citiesLogged && cities != null) {
            log.putSeries("tspCities", cities);
            citiesLogged = true;
        }

        if (state.accepted() && state.currentSolution() != null) {
            tourHistory.add(state.currentSolution().clone());
        }

        if (!tourHistory.isEmpty()) {
            int[] lastTour = tourHistory.getLast();
            List<Integer> tourList = new ArrayList<>();
            for (int city : lastTour) {
                tourList.add(city);
            }
            log.putSeries("tspTour", tourList);
        }
    }
}
