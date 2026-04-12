import { Link } from "react-router-dom";
import "./HomePage.css";

export default function HomePage() {
  return (
    <div className="home-page">
      <div className="home-hero">
        <h1>Scout: optimization puzzle lab</h1>
        <p>
          Scout is an interactive workspace for building problem instances, exploring solution
          behavior, and comparing runs across multiple optimization puzzles and layouts. It
          connects a Java core and Spring backend with a React frontend for live visualization.
        </p>
        <div className="home-cta">
          <Link className="home-btn primary" to="/lab">Start in Lab</Link>
        </div>
      </div>

      <div className="home-sections">
        <section className="home-card">
          <h2>What you can do</h2>
          <ul>
            <li>Browse the catalog of problems, generators, selection rules, and operators.</li>
            <li>Apply templates to load preset configurations and parameters.</li>
            <li>Create or upload instances, then edit graphs and node data interactively.</li>
            <li>Launch runs and runtime studies, then track progress live.</li>
            <li>Compare results across runs with charts and history.</li>
          </ul>
        </section>

        <section className="home-card">
          <h2>How the Lab works</h2>
          <ol>
            <li>Pick a puzzle template or build a configuration from the catalog.</li>
            <li>Use the right panel to set problem instance details and parameters.</li>
            <li>Open the graph modal to add, remove, or drag nodes and update coordinates.</li>
            <li>Set run limits, seeds, and observers, then launch a run.</li>
          </ol>
        </section>

        <section className="home-card">
          <h2>Runs and analysis</h2>
          <p>
            Runs stream progress updates through WebSockets so charts can update in real time.
            Use the Runs view to review histories, compare algorithms, and inspect runtime
            behavior across problems and repetitions.
          </p>
        </section>
      </div>
    </div>
  );
}
