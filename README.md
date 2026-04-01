# Scout

Scout is an optimization laboratory for experimenting with evolutionary algorithms and heuristic optimization methods.
It consists of a Java-based core framework and backend, plus a web-based frontend for visualization.

---

## Requirements

- Java JDK 21
- Node.js (includes npm)

---

## Project structure

```
scout/
├── core/
├── backend/
├── frontend/
├── gradle/
├── build.gradle
├── settings.gradle
└── run-dev.bat
```

---

## Build and test

### Windows

```powershell
.\gradlew.bat test
```

### macOS / Linux

```bash
./gradlew test
```

Backend-only tests:

```powershell
.\gradlew.bat :backend:test
```

---

## Run backend (Spring Boot)

### Windows

```powershell
.\gradlew.bat :backend:bootRun
```

### macOS / Linux

```bash
./gradlew :backend:bootRun
```

Backend runs at:
- http://localhost:8080
- Health check: http://localhost:8080/api/health

---

## Run frontend (React + Vite)

```powershell
cd frontend
npm install
npm run dev
```

Frontend runs at:
- http://localhost:5173

---

## Run both (Windows)

```powershell
.\run-dev.bat
```

---

## Runtime configuration

Executor pools are configurable in:
- `backend/src/main/resources/application.properties`

Default settings:

```
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

## API and WebSocket

- REST API base: `/api`
- WebSocket endpoint: `/ws`
- Run progress: `/topic/run/{runId}`

Common endpoints:
- `/api/catalog`
- `/api/run`

---

## Ports

- Backend: `8080`
- Frontend: `5173`
