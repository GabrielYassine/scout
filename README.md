# Scout

Scout is an optimization laboratory for experimenting with evolutionary algorithms and heuristic optimization methods.
The project consists of a Java-based core and backend, and a web-based frontend for visualization.

---

## Requirements

### Required
- Java JDK 21
- Node.js
---

## Clone the repository

```bash
git clone https://github.com/GabrielYassine/scout
cd scout
```

---

## Build and test

### Windows
```bash
.\gradlew.bat test
```
### macOS / Linux
```bash
./gradlew test
```
---

## Run backend (Spring Boot)

### Windows
```bash
.\gradlew.bat :backend:bootRun
```

### macOS / Linux
```bash
./gradlew :backend:bootRun
```

Backend runs at:  
http://localhost:8080  
Health check: http://localhost:8080/api/health

---

## Run frontend (React + Vite)

```bash
cd frontend  
npm install  
npm run dev
```

Frontend runs at:  
http://localhost:5173

---

## Run both (Windows)

.\run-dev.bat

---

## Project structure

```bash
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

### Ports in use
Backend: 8080  
Frontend: 5173
