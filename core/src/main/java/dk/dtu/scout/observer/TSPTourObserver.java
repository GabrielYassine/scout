package dk.dtu.scout.observer;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
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
