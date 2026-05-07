# Scout

Scout is an optimization laboratory for experimenting with evolutionary algorithms and heuristic optimization methods.

The application makes it possible to configure, run, and visualize optimization experiments. Users can combine different search spaces, benchmark problems, population models, generators, selection rules, observers, and stop conditions to study how algorithmic components affect performance across different problem types.

Scout supports both single optimization runs and repeated runtime studies. During execution, the backend streams progress to the frontend using WebSockets, allowing users to follow the search process live through charts and visualizations. This includes fitness curves, search-space visualizations, TSP/VRP route visualizations, average-run plots, and box plots.

The goal of Scout is to provide a modular framework for implementing optimization algorithms together with a visual interface for comparing their behaviour across problems, representations, and parameter settings.

---

## Project structure

The project consists of three main parts:

- `core` — the Java framework containing search spaces, problems, population models, generators, selection rules, observers, and stop conditions.
- `backend` — a Spring Boot backend that exposes the API, validates run configurations, executes experiments, and streams progress through WebSockets.
- `frontend` — a React + Vite web application for configuring experiments and visualizing results.
```
scout/
├── core/
├── backend/
├── frontend/
├── gradle/
├── build.gradle
├── settings.gradle
└── run-dev.bat
└── run-dev-mac.sh
└──README.md
```

## Requirements

Before running Scout, make sure the following tools are installed:

- Java JDK 17+ (required for Spring Boot / Gradle)
- Node.js 18+ and npm (required for the React/Vite frontend)
- Gradle
---


## Running the application

Scout can be started using the provided development scripts. These scripts start both the Spring Boot backend and the React frontend.

#### Windows
Double-click `run-dev.bat` or run it from PowerShell / CMD:

.\run-dev.bat

#### macOS / Linux
Go to the project root and run:

    chmod +x run-dev-mac.sh
    chmod +x ./gradlew
    ./run-dev-mac.sh

### Manual start
If the development scripts do not work, the backend and frontend can be started manually.

#### 1) Start backend
From the project root, run:
#### macOS / Linux
    ./gradlew :backend:bootRun
#### Windows
    .\gradlew.bat :backend:bootRun

#### 3) Start frontend

    cd frontend
    npm install (if not done before)
    npm run dev



Backend runs at:
- http://localhost:8080
- Health check: http://localhost:8080/api/health

Frontend runs at:
- http://localhost:5173


## Runtime configuration

The backend uses executor pools for handling run requests and executing optimization tasks. These settings can be configured in:

```text
backend/src/main/resources/application.properties
```

Default configuration:

```properties
scout.executors.request.core-pool-size=4
scout.executors.request.max-pool-size=16
scout.executors.request.queue-capacity=200
scout.executors.request.await-termination-seconds=30

scout.executors.run.core-pool-size=4
scout.executors.run.max-pool-size=16
scout.executors.run.queue-capacity=1000
scout.executors.run.await-termination-seconds=60
```

---
