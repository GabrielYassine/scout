import "./HomePage.css";
import ScoutLogo from "../../assets/icons/ScoutLogo.png";
import SelectorImage from "./assets/selector.png";
import RunConfigImage from "./assets/config.png";
import RouteGraphEditorImage from "./assets/routeGraphEditor.png";
import RunProgressImage from "./assets/runProgress.png";

export default function HomePage() {
  return (
    <div className="home-page">
      <div className="top-section">
        <div className="top-section-left">
          <div className="title-container">
            <div className="home-title">
              <span className="highlight-text">SCOUT</span> - Metaheuristic Optimization Framework
            </div>
            <div className="home-subtitle">
              An interactive framework for configuring, running, visualizing,
              and comparing nature-inspired metaheuristics on combinatorial
              optimization problems
            </div>
          </div>
          <div className="features-section">
            <h2 className="features-title">SCOUT Features</h2>
            <ul className="features-list">
              <li>Create and run optimization experiments directly in the browser.</li>
              <li>Choose between different problem types.</li>
              <li>Use ready-made templates or build a custom algorithm from scratch.</li>
              <li>Adjust parameters and stopping conditions before starting a run.</li>
              <li>Follow the progress of a run through live charts and visualizations.</li>
              <li>Compare results across repeated runs and different problem sizes.</li>
              <li>Import, export and edit TSP and VRP instances for route-based experiments.</li>
            </ul>
          </div>
        </div>
        <div className="top-section-right">
          <div className="logo-container">
            <img className="home-logo" src={ScoutLogo} alt="Scout logo" />
          </div>
        </div>
      </div>

      <hr className="rounded home-divider" />

      <div className="guide-section">
        <h2 className="guide-title">How to use SCOUT</h2>
        <div className="guide-layout">
          <aside className="guide-side guide-left"></aside>

          <div className="guide-content">

            <div className="guide-block">
              <div id="build-experiment" className="guide-block-title">
                Build an Experiment
              </div>

              <div className="guide-block-content">
                Experiments in SCOUT are created by choosing the parts that should
                make up a run. The selector groups the available choices by role,
                such as problem type, generator, selection rule, population model,
                stopping condition, and observer. This makes it easier to see which
                parts are available before assembling the final configuration.
              </div>

              <div className="image-container">
                <img
                  className="guide-image"
                  src={SelectorImage}
                  alt="Selector showing available experiment components"
                />
              </div>

              <div id="run-configuration" className="guide-block-subtitle">
                Run configuration
              </div>

              <div className="guide-block-content">
                The selected pieces are placed in the run configuration area.
                Together, they define the full experiment: what problem should be
                solved, which search space is used, how new solutions are generated,
                how solutions are accepted or selected, when the run should stop,
                and which results should be collected for visualization.
              </div>

              <div className="guide-block-content">
                This visual setup is useful because the configuration becomes easier
                to inspect before the run starts. Instead of hiding the algorithm
                setup in a form, SCOUT shows the experiment as a collection of
                connected choices.
              </div>

              <div className="image-container">
                <img
                  className="guide-image"
                  src={RunConfigImage}
                  alt="Run configuration puzzle with selected experiment components"
                />
              </div>
            </div>

            <div className="guide-block">
              <div id="tsp-vrp-instances" className="guide-block-title">
                Work with TSP and VRP Instances
              </div>

              <div className="guide-block-content">
                SCOUT supports route-based optimization problems such as the
                Traveling Salesperson Problem and the Vehicle Routing Problem.
                These problems use coordinates, routes, depots, and customers
                instead of simple bit strings, so the instance itself becomes an
                important part of the experiment.
              </div>

              <div className="guide-block-content">
                Existing instances can be imported, exported, and reused. Custom
                instances can also be created or adjusted with the graph editor,
                making it possible to test algorithms on smaller examples before
                moving to larger benchmark instances.
              </div>

              <div className="image-container">
                <img
                  className="guide-image"
                  src={RouteGraphEditorImage}
                  alt="Graph editor for TSP and VRP instances"
                />
              </div>
            </div>

            <div className="guide-block">
              <div id="run-progress" className="guide-block-title">
                Run Experiments and Follow Progress
              </div>

              <div className="guide-block-content">
                When an experiment starts, SCOUT opens the run page and begins
                streaming progress updates from the backend. This allows the
                optimization process to be followed while it is still running,
                instead of only showing a final result after the computation has
                finished.
              </div>

              <div className="guide-block-content">
                The run page shows important information such as the current
                progress, runtime, best fitness values, and the visual data produced
                by the selected observers. This makes it easier to understand how
                the algorithm behaves over time.
              </div>

              <div className="image-container">
                <img
                  className="guide-image"
                  src={RunProgressImage}
                  alt="Run page showing live progress and visualizations"
                />
              </div>

              <div id="visualizations" className="guide-block-subtitle">
                Explore visualizations
              </div>

              <div className="guide-block-content">
                Different visualizations show different aspects of the search
                process. Fitness graphs show whether the run improves over time,
                route views show how TSP or VRP solutions change, and hypercube
                plots show movement through bit-string search spaces. Additional
                observers can show data such as pheromone values, temperature
                changes, or fitness phase information.
              </div>

              <div className="guide-block-content">
                The purpose of these visualizations is not only to show the final
                solution, but also to make the working principles of the algorithm
                easier to inspect.
              </div>

              <div id="runtime-studies" className="guide-block-subtitle">
                Compare with Runtime Studies
              </div>

              <div className="guide-block-content">
                A single run can show how one configuration behaves, but randomized
                algorithms may behave differently from run to run. Runtime studies
                repeat experiments across multiple runs or problem sizes, which gives
                a more reliable basis for comparison.
              </div>

              <div className="guide-block-content">
                This mode is useful when comparing algorithms, parameter settings, or
                problem sizes. Instead of focusing on one execution, runtime studies
                help summarize performance across repeated experiments.
              </div>
            </div>
          </div>

          <aside className="guide-side guide-right">
            <div className="guide-nav">
              <div className="guide-nav-title">Guide</div>

              <a href="#build-experiment">Build an Experiment</a>
              <a href="#run-configuration" className="guide-nav-sub">
                Run configuration
              </a>

              <a href="#tsp-vrp-instances">Work with TSP and VRP Instances</a>

              <a href="#run-progress">Run Experiments and Follow Progress</a>
              <a href="#visualizations" className="guide-nav-sub">
                Explore visualizations
              </a>
              <a href="#runtime-studies" className="guide-nav-sub">
                Compare with Runtime Studies
              </a>
            </div>
          </aside>
        </div>
      </div>
    </div>
  );
}