package dk.dtu.scout.observer;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.State;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;
import dk.dtu.scout.logging.SeriesMode;
import dk.dtu.scout.TSPInstance;
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
    private TSPInstance instance;
    private State state;
    private boolean includePheromone = false;
    private boolean debugLogged = false;

    @Override
    public String id() {
        return "tsp-tour";
    }

    @Override
    public void init(State state) {
        this.state = state;
        loadCitiesFromState(state);
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
        return List.of(
            new Parameter(
                "includePheromone",
                "Include Pheromone Heatmap",
                "boolean",
                false,
                null,
                null
            )
        );
    }

    public void setCities(List<Map<String, Double>> cities) {
        this.cities = cities;
    }

    private void loadCitiesFromState(State state) {
        if (state == null) {
            return;
        }
        Object problemObj = state.get("problem");
        if (problemObj instanceof TSPProblem tspProblem) {
            this.instance = tspProblem.getInstance();
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

    @Override
    public void configure(Map<String, Object> params) {
        if (params != null) {
            Object pheromoneParam = params.get("includePheromone");
            if (pheromoneParam instanceof Boolean) {
                this.includePheromone = (Boolean) pheromoneParam;
            }
        }
    }

    @Override
    public void onStart(RunState<int[]> state, RunLog log) {
        if (!citiesLogged && cities != null) {
            log.putSeries("tspCities", cities, SeriesMode.LATEST_ONLY);
            citiesLogged = true;
        }
        logTourSnapshot(state, log);
        logPheromoneHeatmap(log);
    }

    @Override
    public void onStep(RunState<int[]> state, RunLog log) {
        logTourSnapshot(state, log);
        logPheromoneHeatmap(log);
    }

    private void logTourSnapshot(RunState<int[]> state, RunLog log) {
        int[] tour = state.currentSolution();
        if (tour == null) {
            tour = state.bestSolution();
        }
        if (tour == null) return;

        tourHistory.add(tour.clone());
        int[] lastTour = tourHistory.getLast();

        Map<String, Object> tourData = new HashMap<>();
        List<Integer> tourList = new ArrayList<>();
        for (int city : lastTour) {
            tourList.add(city);
        }
        tourData.put("tour", tourList);

        if (instance != null) {
            double tourLength = instance.getTourLength(lastTour);
            tourData.put("length", tourLength);
        }

        log.putSeries("tspTour", tourData, SeriesMode.LATEST_ONLY);
    }

    private void logPheromoneHeatmap(RunLog log) {
        if (!includePheromone) return;
        if (this.state == null) return;

        Object pheromoneObj = this.state.get("pheromoneMatrix");
        List<List<Double>> matrixList = null;

        if (pheromoneObj instanceof double[][] pheromoneMatrix) {
            matrixList = new ArrayList<>(pheromoneMatrix.length);
            for (double[] row : pheromoneMatrix) {
                List<Double> rowList = new ArrayList<>(row.length);
                for (double v : row) rowList.add(v);
                matrixList.add(rowList);
            }
        } else if (pheromoneObj instanceof List<?> outer && !outer.isEmpty() && outer.getFirst() instanceof List<?>) {
            matrixList = new ArrayList<>(outer.size());
            for (Object r : outer) {
                if (r instanceof List<?> inner) {
                    List<Double> rowList = new ArrayList<>(inner.size());
                    for (Object v : inner) {
                        if (v instanceof Number n) rowList.add(n.doubleValue());
                    }
                    matrixList.add(rowList);
                }
            }
        } else {
            int dim = 0;
            if (instance != null) dim = instance.getDimension();
            if (dim <= 0) {
                Object dimObj = this.state.get("dimension");
                if (dimObj instanceof Number n) dim = n.intValue();
            }
            if (dim > 0) {
                matrixList = new ArrayList<>(dim);
                for (int i = 0; i < dim; i++) {
                    List<Double> row = new ArrayList<>(dim);
                    for (int j = 0; j < dim; j++) row.add(0.0);
                    matrixList.add(row);
                }
            }
        }

        if (matrixList != null && !matrixList.isEmpty()) {
            log.putSeries("pheromoneHeatmap", matrixList, SeriesMode.LATEST_ONLY);
        }
    }
}
