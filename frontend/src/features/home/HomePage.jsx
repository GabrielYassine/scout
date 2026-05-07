/**
 * Home page for the Scout application.
 * @author s235257
 */

import "./HomePage.css";
import ScoutLogo from "../../assets/icons/ScoutLogo.png";
import SelectorImage from "./assets/selector.png";
import RunConfigImage from "./assets/config.png";
import RouteGraphEditorImage from "./assets/routeGraphEditor.png";
import RunProgressImage from "./assets/runProgress.png";
import RouteImage from "./assets/route.png";
import HypercubeImage from "./assets/hypercube.png";

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
        <div className="guide-title">How to use SCOUT</div>
        <div className="guide-layout">
          <aside className="guide-side guide-left"></aside>

          <div className="guide-content">
            <div className="guide-block">
              <div id="build-experiment" className="guide-block-title">
                Build an Experiment
              </div>

              <div className="guide-block-content">
                Experiments in SCOUT are created by choosing the components that
                should make up a run. The selector groups the available choices by
                role, such as problem type, generator, selection rule, population
                model, stopping condition, and observer.
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
                The selected components are placed in the run configuration area.
                Together, they define what problem is solved, which search space is
                used, how new solutions are generated, how solutions are accepted or
                selected, when the run stops, and which results are collected.
              </div>

              <div className="guide-block-content">
                Templates can be used to load predefined setups. After loading a
                template, the selected components and their parameters can still be
                changed before starting the run.
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
                These problems use coordinates, routes, depots, and customers as
                part of the experiment setup.
              </div>

              <div className="guide-block-content">
                Instances can be imported, exported, and reused. Custom instances
                can also be created or changed with the graph editor. The graph
                editor can be opened from the instance section on the lab page.
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
                When an experiment starts, SCOUT opens the run page and streams
                progress updates from the backend. The run page shows the current
                status, runtime, fitness values, and visualization data from the
                selected observers.
              </div>

              <div className="guide-block-content">
                The run can be followed while it is executing. When the run is
                finished, the same page shows the final result and the collected run
                data.
              </div>

              <div className="image-container">
                <img
                  className="guide-image"
                  src={RunProgressImage}
                  alt="Run page showing live progress and charts"
                />
              </div>

              <div id="visualizations" className="guide-block-subtitle">
                Explore visualizations
              </div>

              <div className="guide-block-content">
                SCOUT provides several visualization types. Visualizations are
                available on the run page when the corresponding observer has been
                selected in the run configuration. SCOUT currently provides observers
                for some linear charts, route visualizations, and a hypercube visualization.
              </div>

              <div className="guide-block-content">
                The route visualization shows locations and route connections for
                TSP and VRP runs. For TSP, the visualization shows one tour through
                all cities. For VRP, it shows several routes starting and ending at
                the depot.
              </div>

              <div className="image-container">
                <img
                  className="guide-image"
                  src={RouteImage}
                  alt="Route visualization showing a TSP or VRP solution"
                />
              </div>

              <div className="guide-block-content">
                The hypercube visualization is available for bit-string runs. It
                projects visited bit strings into a two-dimensional view, so the
                movement of the search can be followed during the run.
              </div>

              <div className="image-container">
                <img
                  className="guide-image"
                  src={HypercubeImage}
                  alt="Hypercube visualization for bit-string search spaces"
                />
              </div>

              <div id="runtime-studies" className="guide-block-subtitle">
                Compare with Runtime Studies
              </div>

              <div className="guide-block-content">
                Runtime studies repeat a configuration across multiple runs or
                problem sizes. This mode can be selected from the lab page before
                starting the experiment.
              </div>

              <div className="guide-block-content">
                The result is shown as aggregated data, such as average performance
                across repetitions or comparisons between different problem sizes.
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